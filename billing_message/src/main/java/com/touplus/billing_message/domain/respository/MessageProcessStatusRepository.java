package com.touplus.billing_message.domain.respository;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MessageProcessStatusRepository {

    private final JdbcTemplate jdbcTemplate;

    public record ProcessRow(
            LocalDate settlementMonth,
            Long expectedTotal,
            Long createCount,
            Long sentCount) {
    }

    public ProcessRow findCurrent() {
        String sql = """
            SELECT settlement_month, expected_total, create_count, sent_count
            FROM message_process
            WHERE message_process_id = 1
            """;
        List<ProcessRow> rows = jdbcTemplate.query(sql, (rs, rowNum) -> new ProcessRow(
                rs.getObject("settlement_month", LocalDate.class),
                rs.getObject("expected_total", Long.class),
                rs.getObject("create_count", Long.class),
                rs.getObject("sent_count", Long.class)
        ));
        return rows.isEmpty() ? null : rows.get(0);
    }

    public int resetForRun(LocalDate settlementMonth, long expectedTotal) {
        String sql = """
            INSERT INTO message_process
              (message_process_id, settlement_month, expected_total, create_count, sent_count, create_message, sent_message)
            VALUES
              (1, ?, ?, 0, 0, 'WAITED', 'WAITED')
            ON DUPLICATE KEY UPDATE
              settlement_month = VALUES(settlement_month),
              expected_total = VALUES(expected_total),
              create_count = 0,
              sent_count = 0,
              create_message = 'WAITED',
              sent_message = 'WAITED'
            """;
        return jdbcTemplate.update(sql, settlementMonth, expectedTotal);
    }

    public int updateExpected(LocalDate settlementMonth, long expectedTotal) {
        String sql = """
            UPDATE message_process
            SET settlement_month = ?, expected_total = ?
            WHERE message_process_id = 1
            """;
        return jdbcTemplate.update(sql, settlementMonth, expectedTotal);
    }

    public int incrementCreate(long delta) {
        String sql = """
            UPDATE message_process
            SET
              create_count = create_count + ?,
              create_message = CASE
                  WHEN create_count + ? >= expected_total THEN 'DONE'
                  WHEN create_count + ? > 0 THEN 'PENDING'
                  ELSE create_message
              END
            WHERE message_process_id = 1
            """;
        return jdbcTemplate.update(sql, delta, delta, delta);
    }

    public int incrementSent(long delta) {
        String sql = """
            UPDATE message_process
            SET
              sent_count = sent_count + ?,
              sent_message = CASE
                  WHEN sent_count + ? >= expected_total THEN 'DONE'
                  WHEN sent_count + ? > 0 THEN 'PENDING'
                  ELSE sent_message
              END
            WHERE message_process_id = 1
            """;
        return jdbcTemplate.update(sql, delta, delta, delta);
    }
}
