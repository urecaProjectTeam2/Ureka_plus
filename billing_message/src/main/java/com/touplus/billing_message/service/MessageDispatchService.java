package com.touplus.billing_message.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DeadlockLoserDataAccessException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.touplus.billing_message.domain.entity.Message;
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

    @Transactional
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

    // 발송 DTO
    public record ProcessResult(Long messageId, boolean success) {
    }

    private static final int MAX_RETRY_COUNT = 3;
    private static final int RETRY_BASE_DELAY_MS = 50;
    private static final int RETRY_RANDOM_DELAY_MS = 100;

    public void dispatchDueMessages() {
        List<Long> messageIds = prepareDispatchWithRetry(LocalDateTime.now());

        if (messageIds.isEmpty()) {
            log.debug("발송 대상 메시지 없음");
            return;
        }

        log.info("메시지 {}건 dispatch 시작", messageIds.size());
        dispatchPreparedMessages(messageIds);
    }

    /**
     * Deadlock 발생 시 재시도하는 prepareDispatch 래퍼
     * - MySQL Deadlock(1213) 발생 시 최대 3회 재시도
     * - 재시도 간 랜덤 지연으로 충돌 확률 감소
     */
    private List<Long> prepareDispatchWithRetry(LocalDateTime now) {
        for (int attempt = 1; attempt <= MAX_RETRY_COUNT; attempt++) {
            try {
                return prepareDispatch(now);
            } catch (DeadlockLoserDataAccessException | CannotAcquireLockException e) {
                log.warn("Deadlock 발생, 재시도 {}/{}: {}", attempt, MAX_RETRY_COUNT, e.getMessage());

                if (attempt == MAX_RETRY_COUNT) {
                    log.error("Deadlock 재시도 한도 초과, 이번 배치 스킵");
                    return List.of();
                }

                // 랜덤 지연으로 충돌 확률 감소
                try {
                    Thread.sleep(RETRY_BASE_DELAY_MS + ThreadLocalRandom.current().nextInt(RETRY_RANDOM_DELAY_MS));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.warn("재시도 대기 중 인터럽트 발생");
                    return List.of();
                }
            }
        }
        return List.of();
    }
}
