package com.touplus.billing_message.service;

import java.time.LocalDate;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 정합성 검증기
 * 원천(billing_snapshot) vs 최종(message_send_log) 개수 비교
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IntegrityChecker {

    private final JdbcTemplate jdbcTemplate;
    private final RecoveryService recoveryService;

    /**
     * 검증 실행
     */
    public void runVerification() {
        log.info("🔍 정합성 검증 시작");
        
        CountResult result = compareCount();
        
        if (result.isMatch()) {
            log.info("✅ 정합성 검증 통과: source={}, target={}", 
                     result.sourceCount(), result.targetCount());
            return;
        }
        
        log.warn("❌ 정합성 불일치: source={}, target={}, diff={}", 
                 result.sourceCount(), result.targetCount(), result.diff());
        
        // 누락 복구 실행
        recoveryService.recoverMissing();
    }

    /**
     * 개수 비교 (원천 vs 최종)
     */
    public CountResult compareCount() {
        LocalDate currentMonth = LocalDate.now().withDayOfMonth(1);
        
        // 원천: billing_snapshot
        Long sourceCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM billing_snapshot WHERE settlement_month >= ?",
            Long.class, currentMonth.minusMonths(1));
        
        // 최종: message_send_log (중간 상태 안 봄!)
        Long targetCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(DISTINCT message_id) FROM message_send_log",
            Long.class);
        
        return new CountResult(
            sourceCount != null ? sourceCount : 0,
            targetCount != null ? targetCount : 0
        );
    }

    /**
     * 개수 비교 결과
     */
    public record CountResult(long sourceCount, long targetCount) {
        public boolean isMatch() {
            return sourceCount == targetCount;
        }
        
        public long diff() {
            return sourceCount - targetCount;
        }
    }
}
