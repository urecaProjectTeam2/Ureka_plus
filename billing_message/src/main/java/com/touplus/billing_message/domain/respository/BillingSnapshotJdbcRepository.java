package com.touplus.billing_message.domain.respository;

import java.time.LocalDate;
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
            INSERT IGNORE INTO billing_snapshot
            (billing_id, settlement_month, user_id, total_price, settlement_details)
            VALUES (?, ?, ?, ?, ?)
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

    /**
     * 전체 스냅샷 조회 (JDBC - JPA 페이징 오버헤드 제거)
     */
    public List<BillingSnapshot> findAll() {
        String sql = """
            SELECT billing_id, settlement_month, user_id, total_price, settlement_details
            FROM billing_snapshot
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new BillingSnapshot(
                rs.getLong("billing_id"),
                rs.getObject("settlement_month", LocalDate.class),
                rs.getLong("user_id"),
                rs.getInt("total_price"),
                rs.getString("settlement_details")
        ));
    }

    /**
     * 전체 카운트 (JDBC)
     */
    public long count() {
        String sql = "SELECT COUNT(*) FROM billing_snapshot";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0L;
    }
}
