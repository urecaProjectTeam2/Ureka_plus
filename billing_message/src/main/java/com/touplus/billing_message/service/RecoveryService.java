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
 * 불일치 발생 시 billing_snapshot을 전체 스캔하되,
 * 실제 누락된 건(send_log 없음)만 골라서 재처리
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
     * billing_snapshot 스코프 전체를 스캔하여 누락분 복구
     */
    public void recoverMissing() {
        LocalDate targetMonth = LocalDate.now().withDayOfMonth(1).minusMonths(1);
        
        log.info("🔧 전체 복구 시작: settlement_month >= {}", targetMonth);
        
        reprocessAllFromBillingSnapshot(targetMonth);
        
        log.info("✅ 전체 복구 완료");
    }

    /**
     * billing_snapshot부터 전체 스캔하여 누락된 건만 재처리
     * - 청크 단위로 읽어서 LEFT JOIN으로 누락 확인
     * - 누락된 건만 골라서 파이프라인 주입
     */
    public void reprocessAllFromBillingSnapshot(LocalDate fromSettlementMonth) {
        long lastId = 0;
        int totalReprocessed = 0;
        
        while (true) {
            // 1. 범위 내의 Billing ID 청크 조회 (단순 스캔용 커서 이동)
            List<Long> chunkIds = jdbcTemplate.queryForList("""
                SELECT billing_id FROM billing_snapshot
                WHERE settlement_month >= ?
                  AND billing_id > ?
                ORDER BY billing_id
                LIMIT ?
            """, Long.class, fromSettlementMonth, lastId, CHUNK_SIZE);
            
            if (chunkIds.isEmpty()) break;
            
            lastId = chunkIds.get(chunkIds.size() - 1); // 다음 커서 이동 (마지막 ID)
            
            // 2. 이 청크 중에서 "누락된 것"만 상세 조회 (Targeting)
            String ids = chunkIds.stream().map(Object::toString).collect(Collectors.joining(","));
            
            List<BillingSnapshot> missingSnapshots = jdbcTemplate.query(
                String.format("""
                    SELECT bs.billing_id, bs.settlement_month, bs.user_id, bs.total_price, bs.settlement_details
                    FROM billing_snapshot bs
                    LEFT JOIN message m ON bs.billing_id = m.billing_id
                    LEFT JOIN message_send_log sl ON m.message_id = sl.message_id
                    WHERE bs.billing_id IN (%s)
                      AND sl.message_id IS NULL  -- 로그가 없는 것만 조회
                """, ids),
                (rs, rowNum) -> new BillingSnapshot(
                    rs.getLong("billing_id"),
                    rs.getDate("settlement_month").toLocalDate(),
                    rs.getLong("user_id"),
                    rs.getInt("total_price"),
                    rs.getString("settlement_details")
                )
            );
            
            if (missingSnapshots.isEmpty()) {
                continue; // 이 청크는 모두 정상
            }
            
            // 3. 누락된 건만 재처리 실행
            List<Long> userIds = missingSnapshots.stream()
                .map(BillingSnapshot::getUserId)
                .distinct()
                .toList();
            
            Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getUserId, u -> u));

            messageProcessor.processBatchWithUsers(missingSnapshots, userMap);
            
            totalReprocessed += missingSnapshots.size();
            log.info("🩹 누락 복구 진행: 구간 내 {}건 발견 및 재처리", missingSnapshots.size());
        }
        
        log.info("✅ 누락 복구 완료: 총 {}건 재처리됨", totalReprocessed);
    }
}
