package com.touplus.billing_message.scheduler;

import com.touplus.billing_message.service.DeadLetterQueueService;
import com.touplus.billing_message.service.DispatchActivationFlag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * DLQ 자동 재처리 스케줄러
 * - 일정 주기로 DLQ에서 메시지를 꺼내 메인 큐로 이동
 * - 최대 재시도 횟수 초과 시 영구 DLQ로 이동
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DlqRetryScheduler {

    private final DeadLetterQueueService deadLetterQueueService;
    private final DispatchActivationFlag dispatchActivationFlag;

    @Value("${message.dlq.retry-batch-size:100}")
    private int retryBatchSize;

    /**
     * 30초마다 DLQ 재처리 시도
     */
    @Scheduled(fixedDelayString = "${message.dlq.retry-interval-ms:30000}")
    public void retryDeadLetterMessages() {
        if (!dispatchActivationFlag.isEnabled()) {
            return;
        }

        Long dlqCount = deadLetterQueueService.getCount();
        if (dlqCount == null || dlqCount == 0) {
            return;
        }

        log.info("DLQ 재처리 시작: 대기 {}건", dlqCount);
        int retried = deadLetterQueueService.retryFromDeadLetter(retryBatchSize);

        if (retried > 0) {
            log.info("DLQ 재처리 완료: {}건 → 메인 큐로 이동", retried);
        }
    }
}
