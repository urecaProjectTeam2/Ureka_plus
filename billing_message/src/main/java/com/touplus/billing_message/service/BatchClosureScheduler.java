package com.touplus.billing_message.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 배치 닫힘 스케줄러
 * 1분마다 Kafka 입력 종료 여부를 확인하고, 30분 무입력 시 검증 실행
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BatchClosureScheduler {

    private final KafkaInputTracker kafkaInputTracker;
    private final IntegrityChecker integrityChecker;

    @Value("${batch.idle-minutes:30}")
    private int idleMinutes;

    private volatile boolean batchClosed = false;

    /**
     * 1분마다 배치 닫힘 체크
     */
    @Scheduled(fixedRate = 60000)
    public void checkBatchClosure() {
        if (batchClosed) {
            return;  // 이미 처리됨
        }

        if (kafkaInputTracker.isIdle(idleMinutes)) {
            batchClosed = true;
            log.info("🔒 배치 닫힘 감지: {}분 무입력", idleMinutes);
            integrityChecker.runVerification();
        }
    }

    /**
     * 새 입력 시 배치 상태 리셋 (Consumer에서 호출)
     */
    public void resetBatchClosure() {
        if (batchClosed) {
            log.info("🔓 새 입력 감지 → 배치 상태 리셋");
            batchClosed = false;
        }
    }

    /**
     * 현재 배치 닫힘 상태
     */
    public boolean isBatchClosed() {
        return batchClosed;
    }
}
