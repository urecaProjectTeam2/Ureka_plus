package com.touplus.billing_message.service;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Kafka 입력 추적기
 * 마지막 입력 시각을 기록하여 배치 닫힘 판단에 사용
 */
@Component
@Slf4j
public class KafkaInputTracker {

    private final AtomicLong lastInputTime = new AtomicLong(System.currentTimeMillis());

    /**
     * Kafka 메시지 수신 시 호출
     */
    public void recordInput() {
        lastInputTime.set(System.currentTimeMillis());
    }

    /**
     * 지정된 시간 동안 입력이 없었는지 확인
     * @param idleMinutes 무입력 기준 시간 (분)
     * @return true면 idle 상태
     */
    public boolean isIdle(long idleMinutes) {
        long elapsed = System.currentTimeMillis() - lastInputTime.get();
        return elapsed >= idleMinutes * 60 * 1000;
    }

    /**
     * 마지막 입력 시각 조회
     */
    public long getLastInputTime() {
        return lastInputTime.get();
    }

    /**
     * 마지막 입력 후 경과 시간 (분)
     */
    public long getIdleMinutes() {
        return (System.currentTimeMillis() - lastInputTime.get()) / 60000;
    }
}
