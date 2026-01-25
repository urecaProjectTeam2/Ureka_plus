package com.touplus.billing_message.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.touplus.billing_message.domain.entity.BillingSnapshot;
import com.touplus.billing_message.domain.entity.User;
import com.touplus.billing_message.domain.respository.UserRepository;
import com.touplus.billing_message.processor.MessageProcessor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 누락 복구 서비스
 * 불일치 발생 시 billing_snapshot 전체를 기존 파이프라인에 재주입
 * Unique 제약이 중복을 무해화함
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RecoveryService {

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final MessageProcessor messageProcessor;

    private static final int CHUNK_SIZE = 1000;

    /**
     * 전체 복구 실행
     * billing_snapshot 스코프 전체를 기존 파이프라인에 재주입
     */
    public void recoverMissing() {
        LocalDate targetMonth = LocalDate.now().withDayOfMonth(1).minusMonths(1);
        
        log.info("🔧 전체 복구 시작: settlement_month >= {}", targetMonth);
        
        reprocessAllFromBillingSnapshot(targetMonth);
        
        log.info("✅ 전체 복구 완료");
    }

    /**
     * billing_snapshot부터 전체 재처리
     * - 청크 단위로 읽어서 기존 messageProcessor로 주입
     * - Unique 제약이 중복을 무해화
     */
    public void reprocessAllFromBillingSnapshot(LocalDate fromSettlementMonth) {
        long lastId = 0;
        int totalProcessed = 0;

        while (true) {
            // billing_snapshot 청크 조회
            List<BillingSnapshot> batch = jdbcTemplate.query("""
                SELECT billing_id, settlement_month, user_id, total_price, settlement_details
                FROM billing_snapshot
                WHERE settlement_month >= ?
                  AND billing_id > ?
                ORDER BY billing_id
                LIMIT ?
                """, 
                (rs, rowNum) -> new BillingSnapshot(
                    rs.getLong("billing_id"),
                    rs.getDate("settlement_month").toLocalDate(),
                    rs.getLong("user_id"),
                    rs.getInt("total_price"),
                    rs.getString("settlement_details")
                ), 
                fromSettlementMonth, lastId, CHUNK_SIZE);

            if (batch.isEmpty()) {
                break;
            }

            // User 일괄 조회
            List<Long> userIds = batch.stream()
                .map(BillingSnapshot::getUserId)
                .distinct()
                .toList();
            
            Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getUserId, u -> u));

            // 기존 파이프라인으로 재주입 (INSERT IGNORE로 중복 안전)
            messageProcessor.processBatchWithUsers(batch, userMap);

            lastId = batch.get(batch.size() - 1).getBillingId();
            totalProcessed += batch.size();
            
            log.info("📦 재처리 진행 중: {}건 완료", totalProcessed);
        }

        log.info("📦 재처리 총 {}건", totalProcessed);
    }
}
