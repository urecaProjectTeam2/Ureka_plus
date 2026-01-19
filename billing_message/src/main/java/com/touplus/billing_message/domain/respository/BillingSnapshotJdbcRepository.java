package com.touplus.billing_message.domain.respository;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.touplus.billing_message.domain.entity.BillingSnapshot;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class BillingSnapshotJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public void batchUpsertByUserMonth(List<BillingSnapshot> snapshots) {

        String sql = """
            INSERT INTO billing_snapshot
            (billing_id, settlement_month, user_id, total_price, settlement_details)
            VALUES (?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                total_price = VALUES(total_price),
                settlement_details = VALUES(settlement_details)
        """;

        jdbcTemplate.batchUpdate(
            sql,
            snapshots,
            snapshots.size(),
            (ps, s) -> {
                ps.setLong(1, s.getBillingId());
                ps.setObject(2, s.getSettlementMonth());
                ps.setLong(3, s.getUserId());
                ps.setInt(4, s.getTotalPrice());
                ps.setString(5, s.getSettlementDetails());
            }
        );
    }
}
