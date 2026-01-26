package com.touplus.billing_message.scheduler;

import com.touplus.billing_message.domain.entity.MessageSnapshot;
import com.touplus.billing_message.domain.entity.MessageType;
import com.touplus.billing_message.domain.respository.MessageJdbcRepository;
import com.touplus.billing_message.domain.respository.MessageSnapshotJdbcRepository;
import com.touplus.billing_message.sender.MessageSender;
import com.touplus.billing_message.sender.SendResult;
import com.touplus.billing_message.service.DeadLetterQueueService;
import com.touplus.billing_message.service.DispatchActivationFlag;
import com.touplus.billing_message.service.MessageProcessStatusService;
import com.touplus.billing_message.service.SendLogBufferService;
import com.touplus.billing_message.service.WaitingQueueService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Redis ZSet 기반 메시지 발송 스케줄러 (다중 스케줄러)
 * - DB 락 없음 (FOR UPDATE SKIP LOCKED 제거)
 * - Redis Lua 스크립트로 원자적 pop → 중복 처리 방지
 * - 5개 스케줄러가 60ms 간격으로 시작하여 병렬 처리
 */
@Component
@Slf4j
public class MessageDispatchScheduler {

    private final WaitingQueueService waitingQueueService;
    private final MessageJdbcRepository messageJdbcRepository;
    private final MessageSender messageSender;
    private final SendLogBufferService sendLogBufferService;
    private final MessageProcessStatusService messageProcessStatusService;
    private final Executor messageDispatchTaskExecutor;
    private final DispatchActivationFlag dispatchActivationFlag;
    private final DeadLetterQueueService deadLetterQueueService;

    public MessageDispatchScheduler(
            WaitingQueueService waitingQueueService,
            MessageJdbcRepository messageJdbcRepository,
            MessageSender messageSender,
            SendLogBufferService sendLogBufferService,
            @Qualifier("messageDispatchTaskExecutor") Executor messageDispatchTaskExecutor,
            DispatchActivationFlag dispatchActivationFlag,
            DeadLetterQueueService deadLetterQueueService,
            MessageProcessStatusService messageProcessStatusService) {
        this.waitingQueueService = waitingQueueService;
        this.messageJdbcRepository = messageJdbcRepository;
        this.messageSender = messageSender;
        this.sendLogBufferService = sendLogBufferService;
        this.messageDispatchTaskExecutor = messageDispatchTaskExecutor;
        this.dispatchActivationFlag = dispatchActivationFlag;
        this.deadLetterQueueService = deadLetterQueueService;
        this.messageProcessStatusService = messageProcessStatusService;
    }

    @Value("${message.dispatch.batch-size:500}")
    private int batchSize;

    @Value("${message.dispatch.chunk-size:200}")
    private int chunkSize;

    @Value("${message.dlq.max-snapshot-retry:5}")
    private int maxSnapshotRetry;

    /**
     * 다중 스케줄러 #1
     */
    @Scheduled(fixedRateString = "${message.dispatch.poll-delay-ms:300}")
    public void dispatch1() {
        doDispatch(1);
    }

    /**
     * 다중 스케줄러 #2 (60ms 지연 시작)
     */
    @Scheduled(fixedRateString = "${message.dispatch.poll-delay-ms:300}", initialDelay = 60)
    public void dispatch2() {
        doDispatch(2);
    }

    /**
     * 다중 스케줄러 #3 (120ms 지연 시작)
     */
    @Scheduled(fixedRateString = "${message.dispatch.poll-delay-ms:300}", initialDelay = 120)
    public void dispatch3() {
        doDispatch(3);
    }

    /**
     * 다중 스케줄러 #4 (180ms 지연 시작)
     */
    @Scheduled(fixedRateString = "${message.dispatch.poll-delay-ms:300}", initialDelay = 180)
    public void dispatch4() {
        doDispatch(4);
    }

    /**
     * 다중 스케줄러 #5 (240ms 지연 시작)
     */
    @Scheduled(fixedRateString = "${message.dispatch.poll-delay-ms:300}", initialDelay = 240)
    public void dispatch5() {
        doDispatch(5);
    }

    /**
     * 실제 발송 처리 로직
     * - Redis Lua 스크립트로 원자적 pop (중복 방지)
     */
    private void doDispatch(int schedulerNo) {
        if (!dispatchActivationFlag.isEnabled()) {
            return;
        }
        List<String> readyIds = waitingQueueService.popReadyMessageIds(batchSize);

        if (readyIds == null || readyIds.isEmpty()) {
            return;
        }

        List<Long> messageIds = readyIds.stream()
                .map(Long::valueOf)
                .toList();

        log.info("[스케줄러#{}] Redis에서 발송 대상 {}건 조회", schedulerNo, messageIds.size());

        // 발송 처리
        dispatchMessages(messageIds);
    }

    /**
     * 메시지 발송 처리 (chunk 단위)
     */
    private void dispatchMessages(List<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return;
        }
        int size = messageIds.size();
        int step = chunkSize > 0 ? chunkSize : size;
        for (int i = 0; i < size; i += step) {
            int end = Math.min(i + step, size);
            dispatchChunk(messageIds.subList(i, end));
        }
    }

    /**
     * 메시지 발송 처리 (병렬)
     */
    private void dispatchChunk(List<Long> messageIds) {
        // 1. Bulk 조회: Message + Snapshot (JOIN)
        Map<Long, MessageJdbcRepository.MessageWithSnapshotDto> joinedMap =
                messageJdbcRepository.findWithSnapshotByIds(messageIds).stream()
                        .collect(Collectors.toMap(
                                MessageJdbcRepository.MessageWithSnapshotDto::messageId,
                                Function.identity()));

        // 3. 병렬 발송
        List<CompletableFuture<SendResultHolder>> futures = messageIds.stream()
                .map(id -> CompletableFuture.supplyAsync(() -> {
                    MessageJdbcRepository.MessageWithSnapshotDto joined = joinedMap.get(id);

                    if (joined == null) {
                        log.warn("메시지 없음: messageId={}", id);
                        return new SendResultHolder(id, false, null, MissingType.MESSAGE);
                    }
                    if (joined.snapshotMessageId() == null) {
                        log.warn("스냅샷 없음: messageId={}", id);
                        return new SendResultHolder(id, false,
                                new MessageJdbcRepository.MessageDto(
                                        joined.messageId(),
                                        joined.billingId(),
                                        joined.userId(),
                                        joined.status(),
                                        joined.scheduledAt(),
                                        joined.retryCount(),
                                        joined.banEndTime()),
                                MissingType.SNAPSHOT);
                    }

                    return sendMessage(
                            id,
                            new MessageJdbcRepository.MessageDto(
                                    joined.messageId(),
                                    joined.billingId(),
                                    joined.userId(),
                                    joined.status(),
                                    joined.scheduledAt(),
                                    joined.retryCount(),
                                    joined.banEndTime()),
                            new MessageSnapshotJdbcRepository.MessageSnapshotDto(
                                    joined.snapshotMessageId(),
                                    joined.snapshotBillingId(),
                                    joined.settlementMonth(),
                                    joined.snapshotUserId(),
                                    joined.userName(),
                                    joined.userEmail(),
                                    joined.userPhone(),
                                    joined.totalPrice(),
                                    joined.settlementDetails(),
                                    joined.messageContent()));
                }, messageDispatchTaskExecutor))
                .toList();

        // 4. 모든 발송 완료 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 5. 결과 처리 (Bulk UPDATE)
        List<Long> successIds = new ArrayList<>();
        List<FailedMessage> failedMessages = new ArrayList<>();
        List<Long> missingSnapshotIds = new ArrayList<>();
        List<Long> missingMessageIds = new ArrayList<>();

        for (CompletableFuture<SendResultHolder> future : futures) {
            try {
                SendResultHolder result = future.get();
                if (result.missingType == MissingType.SNAPSHOT) {
                    missingSnapshotIds.add(result.messageId);
                    continue;
                }
                if (result.missingType == MissingType.MESSAGE) {
                    missingMessageIds.add(result.messageId);
                    continue;
                }
                if (result.success) {
                    successIds.add(result.messageId);
                } else if (result.message != null) {
                    failedMessages.add(new FailedMessage(
                            result.messageId,
                            result.message.retryCount()));
                }
            } catch (Exception e) {
                log.error("결과 수집 실패", e);
            }
        }

        // 6. Bulk UPDATE - 성공
        if (!successIds.isEmpty()) {
            int updated = messageJdbcRepository.bulkMarkSent(successIds);
            messageProcessStatusService.increaseSentCount(updated);
            log.info("발송 성공: {}건", successIds.size());
        }

        // 7. 실패 처리 - Bulk (Redis Pipeline + DB Bulk UPDATE)
        if (!failedMessages.isEmpty()) {
            List<Long> failedIds = failedMessages.stream()
                    .map(FailedMessage::messageId)
                    .toList();

            // Redis Pipeline으로 큐 꼬리에 일괄 추가
            LocalDateTime scheduledAt = waitingQueueService.addToQueueTailBatch(failedIds);

            // DB Bulk UPDATE
            messageJdbcRepository.bulkMarkFailed(failedIds, scheduledAt);

            log.info("발송 실패 → Bulk 큐 꼬리 재큐잉: {}건", failedIds.size());
        }

        // 8. 스냅샷 없음 - Bulk 처리
        if (!missingSnapshotIds.isEmpty()) {
            LocalDateTime retryAt = LocalDateTime.now().plusSeconds(30);

            // DB Bulk UPDATE
            messageJdbcRepository.bulkDefer(missingSnapshotIds, retryAt);

            // Redis Pipeline으로 일괄 추가
            Map<Long, LocalDateTime> retryMap = new HashMap<>();
            for (Long messageId : missingSnapshotIds) {
                retryMap.put(messageId, retryAt);
            }
            waitingQueueService.addToQueueBatch(retryMap, 0);

            log.warn("스냅샷 없음 Bulk 재큐잉: {}건", missingSnapshotIds.size());
        }

        if (!missingMessageIds.isEmpty()) {
            deadLetterQueueService.addBatch(missingMessageIds, "MESSAGE_NOT_FOUND");
            log.error("메시지 없음 → DLQ 이동: {}건", missingMessageIds.size());
        }

        // 9. Redis에서 제거 (이미 pop된 상태이므로 불필요 - 제거)
        // 참고: popReadyMessageIds()에서 Lua 스크립트로 이미 ZREM 됨
    }

    /**
     * 개별 메시지 발송
     */
    private SendResultHolder sendMessage(
            Long messageId,
            MessageJdbcRepository.MessageDto message,
            MessageSnapshotJdbcRepository.MessageSnapshotDto snapshotDto) {

        MessageType messageType = message.retryCount() >= 3 ? MessageType.SMS : MessageType.EMAIL;

        MessageSnapshot snapshot = new MessageSnapshot(
                snapshotDto.messageId(),
                snapshotDto.billingId(),
                snapshotDto.settlementMonth(),
                snapshotDto.userId(),
                snapshotDto.userName(),
                snapshotDto.userEmail(),
                snapshotDto.userPhone(),
                snapshotDto.totalPrice(),
                snapshotDto.settlementDetails(),
                snapshotDto.messageContent());

        try {
            SendResult result = messageSender.send(messageType, snapshot);

            // 발송 로그 버퍼에 추가
            sendLogBufferService.addLog(
                    messageId,
                    message.retryCount(),
                    messageType,
                    result.code(),
                    result.message(),
                    LocalDateTime.now());

            return new SendResultHolder(messageId, result.success(), message, MissingType.NONE);

        } catch (Exception e) {
            log.error("발송 예외: messageId={}", messageId, e);
            return new SendResultHolder(messageId, false, message, MissingType.NONE);
        }
    }

    private enum MissingType {
        NONE,
        MESSAGE,
        SNAPSHOT
    }

    // 발송 결과 홀더
    private record SendResultHolder(
            Long messageId,
            boolean success,
            MessageJdbcRepository.MessageDto message,
            MissingType missingType) {}

    private record FailedMessage(Long messageId, int retryCount) {}
}
