package com.touplus.billing_message.scheduler;

import com.touplus.billing_message.domain.entity.MessageSnapshot;
import com.touplus.billing_message.domain.entity.MessageType;
import com.touplus.billing_message.domain.respository.MessageJdbcRepository;
import com.touplus.billing_message.domain.respository.MessageSnapshotJdbcRepository;
import com.touplus.billing_message.sender.MessageSender;
import com.touplus.billing_message.sender.SendResult;
import com.touplus.billing_message.service.SendLogBufferService;
import com.touplus.billing_message.service.WaitingQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Redis ZSet 기반 메시지 발송 스케줄러
 * - DB 락 없음 (FOR UPDATE SKIP LOCKED 제거)
 * - Redis에서 발송 대상 조회 → 직접 발송
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MessageDispatchScheduler {

    private final WaitingQueueService waitingQueueService;
    private final MessageJdbcRepository messageJdbcRepository;
    private final MessageSnapshotJdbcRepository messageSnapshotJdbcRepository;
    private final MessageSender messageSender;
    private final SendLogBufferService sendLogBufferService;
    private final Executor messageDispatchTaskExecutor;

    @Value("${message.dispatch.batch-size:500}")
    private int batchSize;

    /**
     * Redis 기반 메인 스케줄러
     * - 300ms 간격으로 Redis 폴링
     * - DB 락 없이 Redis에서 발송 대상 조회
     */
    @Scheduled(fixedDelayString = "${message.dispatch.poll-delay-ms:300}")
    public void dispatchFromRedis() {
        List<String> readyIds = waitingQueueService.popReadyMessageIds(batchSize);

        if (readyIds == null || readyIds.isEmpty()) {
            return;
        }

        List<Long> messageIds = readyIds.stream()
                .map(Long::valueOf)
                .toList();

        log.info("Redis에서 발송 대상 {}건 조회", messageIds.size());

        // 발송 처리
        dispatchMessages(messageIds);

        // Redis 제거는 dispatchMessages에서 처리
    }

    /**
     * 메시지 발송 처리 (병렬)
     */
    private void dispatchMessages(List<Long> messageIds) {
        // 1. Bulk 조회: Message
        Map<Long, MessageJdbcRepository.MessageDto> messageMap =
                messageJdbcRepository.findByIds(messageIds).stream()
                        .collect(Collectors.toMap(
                                MessageJdbcRepository.MessageDto::messageId,
                                Function.identity()));

        // 2. Bulk 조회: Snapshot
        Map<Long, MessageSnapshotJdbcRepository.MessageSnapshotDto> snapshotMap =
                messageSnapshotJdbcRepository.findByIds(messageIds).stream()
                        .collect(Collectors.toMap(
                                MessageSnapshotJdbcRepository.MessageSnapshotDto::messageId,
                                Function.identity()));

        // 3. 병렬 발송
        List<CompletableFuture<SendResultHolder>> futures = messageIds.stream()
                .map(id -> CompletableFuture.supplyAsync(() -> {
                    MessageJdbcRepository.MessageDto message = messageMap.get(id);
                    MessageSnapshotJdbcRepository.MessageSnapshotDto snapshotDto = snapshotMap.get(id);

                    if (message == null) {
                        log.warn("메시지 없음: messageId={}", id);
                        return new SendResultHolder(id, false, null, MissingType.MESSAGE);
                    }
                    if (snapshotDto == null) {
                        log.warn("스냅샷 없음: messageId={}", id);
                        return new SendResultHolder(id, false, message, MissingType.SNAPSHOT);
                    }

                    return sendMessage(id, message, snapshotDto);
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

        // 6. Bulk UPDATE
        if (!successIds.isEmpty()) {
            messageJdbcRepository.bulkMarkSent(successIds);
            log.info("발송 성공: {}건", successIds.size());
        }

        // 7. 실패 처리 (재시도 예약)
        for (FailedMessage failed : failedMessages) {
            LocalDateTime retryAt = LocalDateTime.now().plusMinutes(5 * (failed.retryCount + 1));
            messageJdbcRepository.markFailed(failed.messageId, retryAt);
            waitingQueueService.addToQueue(failed.messageId, retryAt);
        }

        if (!failedMessages.isEmpty()) {
            log.info("발송 실패 → 재시도 예약: {}건", failedMessages.size());
        }

        if (!missingSnapshotIds.isEmpty()) {
            LocalDateTime retryAt = LocalDateTime.now().plusSeconds(30);
            for (Long messageId : missingSnapshotIds) {
                messageJdbcRepository.defer(messageId, retryAt);
                waitingQueueService.addToQueue(messageId, retryAt);
            }
            log.warn("스냅샷 없음 재큐잉: {}건", missingSnapshotIds.size());
        }

        if (!missingMessageIds.isEmpty()) {
            log.error("메시지 없음(정합성 오류) 제거 대상: {}건", missingMessageIds.size());
        }

        for (Long id : successIds) {
            waitingQueueService.removeFromQueue(id);
        }
        for (Long id : missingMessageIds) {
            waitingQueueService.removeFromQueue(id);
        }
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
