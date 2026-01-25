package com.touplus.billing_api.domain.repository.message.impl;

import java.time.LocalDate;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.touplus.billing_api.domain.repository.message.MessageDashBoardRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class MessageDashBoardRepositoryImpl implements MessageDashBoardRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public long countBySettlementMonth(LocalDate settlementMonth) {
        String sql = """
            SELECT COUNT(*)
            FROM billing_message.message m
            JOIN billing_message.billing_snapshot b
              ON m.billing_id = b.billing_id
            WHERE b.settlement_month = ?
        """;
        return jdbcTemplate.queryForObject(sql, Long.class, settlementMonth);
    }

    @Override
    public long countBySettlementMonthAndStatus(
            LocalDate settlementMonth,
            String status
    ) {
        String sql = """
            SELECT COUNT(*)
            FROM billing_message.message m
            JOIN billing_message.billing_snapshot b
              ON m.billing_id = b.billing_id
            WHERE b.settlement_month = ?
              AND m.status = ?
        """;
        return jdbcTemplate.queryForObject(
                sql,
                Long.class,
                settlementMonth,
                status
        );
    }
    
    @Override
    public long countByStatusAndRetry() {
        String sql = """
            SELECT COUNT(*) 
            FROM billing_message.message 
            WHERE status = 'WAITED' 
              AND retry_count >= 1;
        """;

        return jdbcTemplate.queryForObject(
            sql,
            Long.class
        );
    }
    
    @Override
    public long countBySMS() {
        String sql = """
            SELECT COUNT(*) 
            FROM billing_message.message 
            WHERE status = 'SENT' 
              AND retry_count >= 2;
        """;

        return jdbcTemplate.queryForObject(
            sql,
            Long.class
        );
    }

}
