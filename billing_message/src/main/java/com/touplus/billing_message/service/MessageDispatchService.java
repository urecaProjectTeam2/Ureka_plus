package com.touplus.billing_message.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import com.touplus.billing_message.domain.entity.Message;
import com.touplus.billing_message.domain.entity.MessageType;
import com.touplus.billing_message.domain.respository.MessageRepository;
import com.touplus.billing_message.domain.respository.MessageJdbcRepository;

import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MessageDispatchService {

    private final MessageClaimService messageClaimService;
    private final MessageProcessService messageProcessService;
    private final TaskExecutor messageDispatchTaskExecutor;
    private final MessageSnapshotService messageSnapshotService;
    private final MessageJdbcRepository messageJdbcRepository;

    public MessageDispatchService(
            MessageClaimService messageClaimService,
            MessageProcessService messageProcessService,
            MessageSnapshotService messageSnapshotService,
            MessageRepository messageRepository,
            MessageJdbcRepository messageJdbcRepository,
            @Qualifier("messageDispatchTaskExecutor") TaskExecutor messageTaskExecutor) {
        this.messageClaimService = messageClaimService;
        this.messageProcessService = messageProcessService;
        this.messageSnapshotService = messageSnapshotService;
        this.messageJdbcRepository = messageJdbcRepository;
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
        // CompletableFuture로 모든 발송 완료 대기 후 Bulk UPDATE (JDBC 버전)
        List<CompletableFuture<ProcessResult>> futures = messageIds.stream()
                .map(id -> CompletableFuture.supplyAsync(
                        () -> processAndReturnResultJdbc(id),
                        messageDispatchTaskExecutor))
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

    public void dispatchDueMessages() {

        List<Long> messageIds = prepareDispatch(LocalDateTime.now());

        if (messageIds.isEmpty()) {
            log.debug("발송 대상 메시지 없음");
            return;
        }

        log.info("메시지 {}건 dispatch 시작", messageIds.size());
        dispatchPreparedMessages(messageIds);
    }
}
