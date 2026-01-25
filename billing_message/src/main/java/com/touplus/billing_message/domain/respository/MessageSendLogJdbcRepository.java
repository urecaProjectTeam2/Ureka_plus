package com.touplus.billing_message.domain.respository;

import com.touplus.billing_message.domain.entity.MessageType;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class MessageSendLogJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Bulk INSERT - 여러 SendLog를 한 번에 저장
     */
    public void bulkInsert(List<SendLogDto> logs) {
        if (logs.isEmpty()) return;

        String sql = """
            INSERT IGNORE INTO message_send_log
            (message_id, retry_no, message_type, provider_response_code, provider_response_message, sent_at)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        jdbcTemplate.batchUpdate(
                sql,
                logs,
                logs.size(),
                (ps, log) -> {
                    ps.setLong(1, log.messageId());
                    ps.setInt(2, log.retryNo());
                    ps.setString(3, log.messageType().name());
                    ps.setString(4, log.providerResponseCode());
                    ps.setString(5, log.providerResponseMessage());
                    ps.setTimestamp(6, Timestamp.valueOf(log.sentAt()));
                });
    }

    /**
     * SendLog DTO
     */
    public record SendLogDto(
            Long messageId,
            int retryNo,
            MessageType messageType,
            String providerResponseCode,
            String providerResponseMessage,
            LocalDateTime sentAt
    ) {}
}
