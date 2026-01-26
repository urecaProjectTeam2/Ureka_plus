package com.touplus.billing_api.admin.repository.impl;

import java.time.LocalDate;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.touplus.billing_api.admin.enums.ProcessType;
import com.touplus.billing_api.admin.repository.MessageProcessRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class MessageProcessRepositoryImpl implements MessageProcessRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public ProcessType findLatestCreateMessageStatus() {
        String sql = "SELECT create_message FROM billing_message.message_process ORDER BY settlement_month ASC LIMIT 1";

        return jdbcTemplate.queryForObject(
            sql,
            (rs, rowNum) -> {
                String val = rs.getString("create_message");
                return val != null ? ProcessType.valueOf(val.trim().toUpperCase()) : ProcessType.WAITED;
            }
        );
    }

    @Override
    public ProcessType findLatestSentMessageStatus() {
        String sql = "SELECT sent_message FROM billing_message.message_process ORDER BY settlement_month ASC LIMIT 1";

        return jdbcTemplate.queryForObject(
            sql,
            (rs, rowNum) -> {
                String val = rs.getString("sent_message");
                return val != null ? ProcessType.valueOf(val.trim().toUpperCase()) : ProcessType.WAITED;
            }
        );
    }

    @Override
    public long countCreateMessage(LocalDate settlementMonth) {
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
    public long countSentMessage(LocalDate settlementMonth) {
        String sql = """
            SELECT COUNT(*)
            FROM billing_message.message m
            JOIN billing_message.billing_snapshot b
              ON m.billing_id = b.billing_id
            WHERE b.settlement_month = ?
              AND m.status = 'SENT'
        """;
        return jdbcTemplate.queryForObject(sql, Long.class, settlementMonth);
    }

    @Override
    public long countTotal() {
        String sql = """
           SELECT count(*) FROM billing_message.users;
        """;
        return jdbcTemplate.queryForObject(sql, Long.class);
    }
}
