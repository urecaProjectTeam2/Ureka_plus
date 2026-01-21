package com.touplus.billing_message.domain.respository;

import com.touplus.billing_message.domain.entity.Message;
import com.touplus.billing_message.domain.entity.MessageStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class MessageJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public void batchInsert(List<Message> messages) {
        String sql = """
                    INSERT INTO message
                    (billing_id, user_id, status, scheduled_at, retry_count)
                    VALUES (?, ?, ?, ?, ?)
                """;

        jdbcTemplate.batchUpdate(
                sql,
                messages,
                messages.size(),
                (ps, m) -> {
                    ps.setLong(1, m.getBillingId());
                    ps.setLong(2, m.getUserId());
                    ps.setString(3, m.getStatus().name());
                    ps.setTimestamp(4, m.getScheduledAt() != null ? Timestamp.valueOf(m.getScheduledAt()) : null);
                    ps.setInt(5, m.getRetryCount());
                });
    }

    /**
     * Bulk SENT 상태 업데이트 (JDBC)
     */
    public int bulkMarkSent(List<Long> messageIds) {
        if (messageIds.isEmpty())
            return 0;

        String placeholders = messageIds.stream()
                .map(id -> "?")
                .collect(Collectors.joining(","));

        String sql = "UPDATE message SET status = 'SENT' WHERE message_id IN (" + placeholders
                + ") AND status = 'CREATED'";

        return jdbcTemplate.update(sql, messageIds.toArray());
    }

    /**
     * Bulk 메시지 조회 (JDBC) - ID 목록으로 조회
     */
    public List<MessageDto> findByIds(List<Long> messageIds) {
        if (messageIds.isEmpty())
            return Collections.emptyList();

        String placeholders = messageIds.stream()
                .map(id -> "?")
                .collect(Collectors.joining(","));

        String sql = "SELECT message_id, billing_id, user_id, status, scheduled_at, retry_count " +
                "FROM message WHERE message_id IN (" + placeholders + ")";

        return jdbcTemplate.query(sql, messageIds.toArray(), (rs, rowNum) -> new MessageDto(
                rs.getLong("message_id"),
                rs.getLong("billing_id"),
                rs.getLong("user_id"),
                MessageStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("scheduled_at") != null
                        ? rs.getTimestamp("scheduled_at").toLocalDateTime()
                        : null,
                rs.getInt("retry_count")));
    }

    /**
     * 메시지 단건 조회 (JDBC)
     */
    public MessageDto findById(Long messageId) {
        String sql = "SELECT message_id, billing_id, user_id, status, scheduled_at, retry_count " +
                "FROM message WHERE message_id = ?";

        List<MessageDto> results = jdbcTemplate.query(sql, new Object[] { messageId }, (rs, rowNum) -> new MessageDto(
                rs.getLong("message_id"),
                rs.getLong("billing_id"),
                rs.getLong("user_id"),
                MessageStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("scheduled_at") != null
                        ? rs.getTimestamp("scheduled_at").toLocalDateTime()
                        : null,
                rs.getInt("retry_count")));

        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * 실패 상태 업데이트 (JDBC)
     */
    public int markFailed(Long messageId, LocalDateTime scheduledAt) {
        String sql = "UPDATE message SET status = 'WAITED', retry_count = retry_count + 1, scheduled_at = ? WHERE message_id = ?";
        return jdbcTemplate.update(sql, Timestamp.valueOf(scheduledAt), messageId);
    }

    /**
     * 발송 연기 (JDBC)
     */
    public int defer(Long messageId, LocalDateTime scheduledAt) {
        String sql = "UPDATE message SET status = 'WAITED', scheduled_at = ? WHERE message_id = ?";
        return jdbcTemplate.update(sql, Timestamp.valueOf(scheduledAt), messageId);
    }

    /**
     * 메시지 DTO (Entity 대체용)
     */
    public record MessageDto(
            Long messageId,
            Long billingId,
            Long userId,
            MessageStatus status,
            LocalDateTime scheduledAt,
            Integer retryCount) {
    }
}
