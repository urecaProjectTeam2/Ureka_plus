package com.touplus.billing_message.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DeadlockLoserDataAccessException;

import com.touplus.billing_message.domain.entity.MessageSnapshot;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.touplus.billing_message.domain.entity.Message;
import com.touplus.billing_message.domain.entity.MessageStatus;
import com.touplus.billing_message.domain.entity.MessageType;
import com.touplus.billing_message.domain.respository.MessageRepository;
import com.touplus.billing_message.domain.respository.MessageJdbcRepository;
import com.touplus.billing_message.domain.respository.MessageSnapshotJdbcRepository;

import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MessageDispatchService {

    private final MessageClaimService messageClaimService;
    private final MessageProcessService messageProcessService;
    private final Executor messageDispatchTaskExecutor;
    private final MessageSnapshotService messageSnapshotService;
    private final MessageJdbcRepository messageJdbcRepository;
    private final MessageSnapshotJdbcRepository messageSnapshotJdbcRepository;

    public MessageDispatchService(
            MessageClaimService messageClaimService,
            MessageProcessService messageProcessService,
            MessageSnapshotService messageSnapshotService,
            MessageRepository messageRepository,
            MessageJdbcRepository messageJdbcRepository,
            MessageSnapshotJdbcRepository messageSnapshotJdbcRepository,
            @Qualifier("messageDispatchTaskExecutor") Executor messageTaskExecutor) {
        this.messageClaimService = messageClaimService;
        this.messageProcessService = messageProcessService;
        this.messageSnapshotService = messageSnapshotService;
        this.messageJdbcRepository = messageJdbcRepository;
        this.messageSnapshotJdbcRepository = messageSnapshotJdbcRepository;
        this.messageDispatchTaskExecutor = messageTaskExecutor;
    }

    /**
     * 메시지 준비 + 데이터 반환 (DB 재조회 제거)
     * - claim한 Message 엔티티를 DTO로 변환
     * - 생성한 Snapshot 엔티티를 DTO로 변환
     * - 이미 존재하는 Snapshot도 조회하여 포함 (retry 메시지 처리용)
     * - dispatchPreparedBatch()에서 바로 사용 가능
     */
    @Transactional
    public PreparedBatch prepareDispatchWithData(LocalDateTime now) {

        List<Message> messages = messageClaimService.claimNextMessagesAsEntities(now);

        if (messages.isEmpty()) {
            return new PreparedBatch(Map.of(), Map.of());
        }

        // Message 엔티티 → DTO 변환
        // 주의: markCreatedByIds()로 DB는 CREATED지만 엔티티 객체는 WAITED
        //       따라서 status를 CREATED로 강제 설정
        Map<Long, MessageJdbcRepository.MessageDto> messageMap = new HashMap<>();
        List<Long> messageIds = new ArrayList<>();
        for (Message m : messages) {
            messageIds.add(m.getMessageId());
            messageMap.put(m.getMessageId(), new MessageJdbcRepository.MessageDto(
                    m.getMessageId(),
                    m.getBillingId(),
                    m.getUserId(),
                    MessageStatus.CREATED,  // DB와 동기화 (엔티티는 WAITED지만 DB는 CREATED)
                    m.getScheduledAt(),
                    m.getRetryCount(),
                    m.getBanEndTime()
            ));
        }

        // Snapshot 생성 (신규만)
        messageSnapshotService.createSnapshotsBatchAndReturn(messages, MessageType.EMAIL);

        // 모든 Snapshot 조회 (신규 + 기존 retry 메시지용)
        Map<Long, MessageSnapshotJdbcRepository.MessageSnapshotDto> snapshotMap =
                messageSnapshotJdbcRepository.findByIds(messageIds)
                        .stream()
                        .collect(Collectors.toMap(
                                MessageSnapshotJdbcRepository.MessageSnapshotDto::messageId,
                                Function.identity()
                        ));

        log.debug("PreparedBatch 생성: Message {}건, Snapshot {}건", messageMap.size(), snapshotMap.size());
        return new PreparedBatch(messageMap, snapshotMap);
    }

    @Transactional
    @Deprecated // prepareDispatchWithData() 사용 권장
    public List<Long> prepareDispatch(LocalDateTime now) {

        List<Message> messages = messageClaimService.claimNextMessagesAsEntities(now);

        if (messages.isEmpty()) {
            return List.of();
        }

        messageSnapshotService.createSnapshotsBatch(messages, MessageType.EMAIL);

        return messages.stream()
                .map(Message::getMessageId)
                .toList();
    }

    /**
     * 준비된 배치 데이터로 발송 처리 (DB 재조회 없음!)
     */
    public void dispatchPreparedBatch(PreparedBatch batch) {
        if (batch.isEmpty()) {
            return;
        }

        Map<Long, MessageJdbcRepository.MessageDto> messageMap = batch.messageMap();
        Map<Long, MessageSnapshotJdbcRepository.MessageSnapshotDto> snapshotMap = batch.snapshotMap();
        List<Long> messageIds = batch.getMessageIds();

        // 비동기 발송 (DB 조회 없이 메모리에서 처리)
        List<CompletableFuture<ProcessResult>> futures = messageIds.stream()
                .map(id -> CompletableFuture.supplyAsync(() -> {
                    MessageJdbcRepository.MessageDto message = messageMap.get(id);
                    MessageSnapshotJdbcRepository.MessageSnapshotDto snapshot = snapshotMap.get(id);

                    if (message == null) {
                        log.warn("메시지 없음 messageId={}", id);
                        return CompletableFuture.completedFuture(new ProcessResult(id, false));
                    }

                    return messageProcessService.processMessageWithDataAsync(message, snapshot);
                }, messageDispatchTaskExecutor)
                        .thenCompose(future -> future))
                .toList();

        // 모든 발송 완료 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 결과 수집
        List<Long> sentIds = new ArrayList<>();

        for (CompletableFuture<ProcessResult> future : futures) {
            try {
                ProcessResult result = future.get();
                if (result != null && result.success()) {
                    sentIds.add(result.messageId());
                }
            } catch (Exception e) {
                log.error("결과 수집 실패", e);
            }
        }

        // Bulk UPDATE (JDBC)
        if (!sentIds.isEmpty()) {
            messageJdbcRepository.bulkMarkSent(sentIds);
            log.info("Bulk SENT 처리 (JDBC): {}건", sentIds.size());
        }
    }

    @Deprecated // dispatchPreparedBatch() 사용 권장
    public void dispatchPreparedMessages(List<Long> messageIds) {
        // ========================================
        // Bulk Fetch: N+1 문제 해결
        // 기존: 500건 × 2회 = 1,000번 DB 조회
        // 개선: 2번 DB 조회 (Message 1회 + Snapshot 1회)
        // ========================================

        // 1. Bulk 조회: Message (1회)
        Map<Long, MessageJdbcRepository.MessageDto> messageMap = messageJdbcRepository.findByIds(messageIds)
                .stream()
                .collect(Collectors.toMap(MessageJdbcRepository.MessageDto::messageId, Function.identity()));

        // 2. Bulk 조회: Snapshot (1회)
        Map<Long, MessageSnapshotJdbcRepository.MessageSnapshotDto> snapshotMap = messageSnapshotJdbcRepository.findByIds(messageIds)
                .stream()
                .collect(Collectors.toMap(MessageSnapshotJdbcRepository.MessageSnapshotDto::messageId, Function.identity()));

        log.debug("Bulk Fetch 완료: Message {}건, Snapshot {}건", messageMap.size(), snapshotMap.size());

        // 3. 비동기 발송 (DB 조회 없이 메모리에서 처리)
        List<CompletableFuture<ProcessResult>> futures = messageIds.stream()
                .map(id -> CompletableFuture.supplyAsync(() -> {
                    MessageJdbcRepository.MessageDto message = messageMap.get(id);
                    MessageSnapshotJdbcRepository.MessageSnapshotDto snapshot = snapshotMap.get(id);

                    if (message == null) {
                        log.warn("메시지 없음 messageId={}", id);
                        return CompletableFuture.completedFuture(new ProcessResult(id, false));
                    }

                    // 이미 조회된 데이터로 처리 (DB 조회 없음!)
                    return messageProcessService.processMessageWithDataAsync(message, snapshot);
                        }, messageDispatchTaskExecutor)
                        .thenCompose(future -> future))
                .toList();

        // 4. 모든 발송 완료 대기 (논블로킹 - 스레드는 이미 반환됨)
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 5. 결과 수집
        List<Long> sentIds = new ArrayList<>();

        for (CompletableFuture<ProcessResult> future : futures) {
            try {
                ProcessResult result = future.get();
                if (result != null && result.success()) {
                    sentIds.add(result.messageId());
                }
            } catch (Exception e) {
                log.error("결과 수집 실패", e);
            }
        }

        // 6. Bulk UPDATE (JDBC)
        if (!sentIds.isEmpty()) {
            messageJdbcRepository.bulkMarkSent(sentIds);
            log.info("Bulk SENT 처리 (JDBC): {}건", sentIds.size());
        }
    }

    // JDBC 발송 처리 후 결과 반환
    private ProcessResult processAndReturnResultJdbc(Long messageId) {
        try {
            return messageProcessService.processMessageAndReturnResultJdbc(messageId);
        } catch (Exception e) {
            log.error("메시지 처리 실패 (JDBC) messageId={}", messageId, e);
            return new ProcessResult(messageId, false);
        }
    }

    // 발송 결과 DTO
    public record ProcessResult(Long messageId, boolean success) {
    }

    /**
     * 준비된 배치 데이터 (DB 재조회 없이 바로 발송 가능)
     */
    public record PreparedBatch(
            Map<Long, MessageJdbcRepository.MessageDto> messageMap,
            Map<Long, MessageSnapshotJdbcRepository.MessageSnapshotDto> snapshotMap
    ) {
        public boolean isEmpty() {
            return messageMap.isEmpty();
        }

        public List<Long> getMessageIds() {
            return new ArrayList<>(messageMap.keySet());
        }
    }

    private static final int MAX_RETRY_COUNT = 3;

    public void dispatchDueMessages() {
        PreparedBatch batch = prepareDispatchWithRetry(LocalDateTime.now());

        if (batch.isEmpty()) {
            log.debug("발송 대상 메시지 없음");
            return;
        }

        log.info("메시지 {}건 dispatch 시작 (DB 재조회 없음)", batch.messageMap().size());
        dispatchPreparedBatch(batch);
    }

    /**
     * Deadlock 발생 시 재시도하는 prepareDispatchWithData 래퍼
     * - MySQL Deadlock(1213) 발생 시 최대 3회 재시도
     */
    private PreparedBatch prepareDispatchWithRetry(LocalDateTime now) {
        for (int attempt = 1; attempt <= MAX_RETRY_COUNT; attempt++) {
            try {
                return prepareDispatchWithData(now);
            } catch (DeadlockLoserDataAccessException | CannotAcquireLockException e) {
                log.warn("Deadlock 발생, 재시도 {}/{}: {}", attempt, MAX_RETRY_COUNT, e.getMessage());

                if (attempt == MAX_RETRY_COUNT) {
                    log.error("Deadlock 재시도 한도 초과, 이번 배치 스킵");
                    return new PreparedBatch(Map.of(), Map.of());
                }
            }
        }
        return new PreparedBatch(Map.of(), Map.of());
    }
}
