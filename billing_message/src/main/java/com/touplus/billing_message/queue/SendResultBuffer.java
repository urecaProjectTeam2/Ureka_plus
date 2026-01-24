package com.touplus.billing_message.queue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

import com.touplus.billing_message.domain.entity.MessageType;
import com.touplus.billing_message.domain.respository.MessageJdbcRepository;
import com.touplus.billing_message.domain.respository.MessageSendLogJdbcRepository;
import com.touplus.billing_message.domain.respository.MessageSendLogJdbcRepository.SendLogDto;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 발송 결과 버퍼
 * 결과를 모아서 Bulk INSERT/UPDATE 수행
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SendResultBuffer {

    private final MessageJdbcRepository messageJdbcRepository;
    private final MessageSendLogJdbcRepository messageSendLogJdbcRepository;

    private final ConcurrentLinkedQueue<SendResult> successQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<SendLogDto> logQueue = new ConcurrentLinkedQueue<>();
    private final AtomicInteger successCount = new AtomicInteger(0);

    private static final int FLUSH_THRESHOLD = 1000;

    /**
     * 발송 성공 결과 추가
     */
    public void addSuccess(Long messageId, MessageType type, String code, String message) {
        successQueue.offer(new SendResult(messageId, true));
        logQueue.offer(new SendLogDto(messageId, 0, type, code, message, LocalDateTime.now()));
        
        int count = successCount.incrementAndGet();
        if (count >= FLUSH_THRESHOLD) {
            flush();
        }
    }

    /**
     * 발송 실패 결과 추가 (로그만 기록, 상태는 변경 안함)
     */
    public void addFailure(Long messageId, MessageType type, String code, String message) {
        logQueue.offer(new SendLogDto(messageId, 0, type, code, message, LocalDateTime.now()));
    }

    /**
     * 버퍼 플러시 - Bulk DB 반영
     */
    public synchronized void flush() {
        // 성공한 메시지 ID 수집
        List<Long> sentIds = new ArrayList<>();
        SendResult result;
        while ((result = successQueue.poll()) != null) {
            if (result.success()) {
                sentIds.add(result.messageId());
            }
        }

        // 로그 수집
        List<SendLogDto> logs = new ArrayList<>();
        SendLogDto logDto;
        while ((logDto = logQueue.poll()) != null) {
            logs.add(logDto);
        }

        successCount.set(0);

        // Bulk UPDATE (CREATED → SENT)
        if (!sentIds.isEmpty()) {
            int updated = messageJdbcRepository.bulkMarkSent(sentIds);
            log.info("Bulk SENT 처리: {}건", updated);
        }

        // Bulk INSERT (send_log)
        if (!logs.isEmpty()) {
            messageSendLogJdbcRepository.bulkInsert(logs);
            log.info("Bulk 로그 저장: {}건", logs.size());
        }
    }

    /**
     * 애플리케이션 종료 시 남은 버퍼 처리
     */
    @PreDestroy
    public void shutdown() {
        log.info("SendResultBuffer 종료 - 남은 버퍼 처리");
        flush();
    }

    /**
     * 현재 버퍼 크기
     */
    public int getBufferSize() {
        return successCount.get();
    }

    public record SendResult(Long messageId, boolean success) {}
}
