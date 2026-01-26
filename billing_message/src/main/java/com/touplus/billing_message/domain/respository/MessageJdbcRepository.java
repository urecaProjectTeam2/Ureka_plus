package com.touplus.billing_message.domain.respository;

import com.touplus.billing_message.domain.entity.Message;
import com.touplus.billing_message.domain.entity.MessageStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class MessageJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Batch Insert (INSERT IGNORE)
     * 
     * @return 성공적으로 INSERT된 건수 (중복은 0, 성공은 1)
     */
    public int batchInsert(List<Message> messages) {
        if (messages.isEmpty()) {
            return 0;
        }

        String sql = """
            INSERT IGNORE INTO message
            (billing_id, user_id, status, scheduled_at, retry_count, ban_start_time, ban_end_time)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        int[][] results = jdbcTemplate.batchUpdate(
            sql,
            messages,
            messages.size(),
            (ps, m) -> {
                ps.setLong(1, m.getBillingId());
                ps.setLong(2, m.getUserId());
                ps.setString(3, m.getStatus().name());
                ps.setTimestamp(
                    4,
                    m.getScheduledAt() != null ? Timestamp.valueOf(m.getScheduledAt()) : null
                );
                ps.setInt(5, m.getRetryCount());
                ps.setTime(
                    6,
                    m.getBanStartTime() != null ? Time.valueOf(m.getBanStartTime()) : null
                );
                ps.setTime(
                    7,
                    m.getBanEndTime() != null ? Time.valueOf(m.getBanEndTime()) : null
                );
            }
        );

        int successCount = 0;
        for (int[] batch : results) {
            for (int affected : batch) {
                if (affected > 0 || affected == java.sql.Statement.SUCCESS_NO_INFO) {
                    successCount++;
                }
            }
        }
        return successCount;
    }


    /**
     * Bulk SENT 상태 업데이트 (JDBC)
     */
    /*public int bulkMarkSent(List<Long> messageIds) {
        if (messageIds.isEmpty())
            return 0;

        String placeholders = messageIds.stream()
                .map(id -> "?")
                .collect(Collectors.joining(","));

        String sql = "UPDATE message SET status = 'SENT' WHERE message_id IN (" + placeholders
                + ") AND status = 'CREATED'";

        return jdbcTemplate.update(sql, messageIds.toArray());
    }*/

    public int bulkMarkSent(List<Long> messageIds) {
        if (messageIds.isEmpty()) return 0;

        int totalUpdated = 0;
        int batchSize = 500;
        // messageIds를 BATCH_SIZE 단위로 나눔
        for (int i = 0; i < messageIds.size(); i += batchSize) {
            int end = Math.min(i + batchSize, messageIds.size());
            List<Long> batch = messageIds.subList(i, end);

            String placeholders = batch.stream()
                    .map(id -> "?")
                    .collect(Collectors.joining(","));

            String sql = "UPDATE message SET status = 'SENT' WHERE message_id IN (" + placeholders + ") AND status IN ('WAITED','CREATED')";

            totalUpdated += jdbcTemplate.update(sql, batch.toArray());
        }

        return totalUpdated;
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

        String sql = "SELECT message_id, billing_id, user_id, status, scheduled_at, retry_count, ban_start_time, ban_end_time " +
                "FROM message WHERE message_id IN (" + placeholders + ")";

        return jdbcTemplate.query(sql, messageIds.toArray(), (rs, rowNum) -> new MessageDto(
                rs.getLong("message_id"),
                rs.getLong("billing_id"),
                rs.getLong("user_id"),
                MessageStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("scheduled_at") != null
                        ? rs.getTimestamp("scheduled_at").toLocalDateTime()
                        : null,
                rs.getInt("retry_count"),
                rs.getTime("ban_start_time") != null
                        ? rs.getTime("ban_start_time").toLocalTime()
                        : null,
                rs.getTime("ban_end_time") != null
                        ? rs.getTime("ban_end_time").toLocalTime()
                        : null));
    }

    /**
     * 메시지 + 스냅샷 통합 조회 (LEFT JOIN)
     */
    public List<MessageWithSnapshotDto> findWithSnapshotByIds(List<Long> messageIds) {
        if (messageIds.isEmpty()) {
            return Collections.emptyList();
        }

        String placeholders = messageIds.stream()
                .map(id -> "?")
                .collect(Collectors.joining(","));

        String sql = """
                SELECT m.message_id, m.billing_id, m.user_id, m.status, m.scheduled_at, m.retry_count, m.ban_start_time, m.ban_end_time,
                       s.message_id AS s_message_id, s.billing_id AS s_billing_id, s.settlement_month, s.user_id AS s_user_id,
                       s.user_name, s.user_email, s.user_phone, s.total_price, s.settlement_details, s.message_content
                FROM message m
                LEFT JOIN message_snapshot s ON s.message_id = m.message_id
                WHERE m.message_id IN (%s)
                """.formatted(placeholders);

        return jdbcTemplate.query(sql, messageIds.toArray(), (rs, rowNum) -> new MessageWithSnapshotDto(
                rs.getLong("message_id"),
                rs.getLong("billing_id"),
                rs.getLong("user_id"),
                MessageStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("scheduled_at") != null
                        ? rs.getTimestamp("scheduled_at").toLocalDateTime()
                        : null,
                rs.getInt("retry_count"),
                rs.getTime("ban_start_time") != null
                        ? rs.getTime("ban_start_time").toLocalTime()
                        : null,
                rs.getTime("ban_end_time") != null
                        ? rs.getTime("ban_end_time").toLocalTime()
                        : null,
                rs.getObject("s_message_id") != null ? rs.getLong("s_message_id") : null,
                rs.getObject("s_billing_id") != null ? rs.getLong("s_billing_id") : null,
                rs.getObject("settlement_month", java.time.LocalDate.class),
                rs.getObject("s_user_id") != null ? rs.getLong("s_user_id") : null,
                rs.getString("user_name"),
                rs.getString("user_email"),
                rs.getString("user_phone"),
                rs.getObject("total_price") != null ? rs.getInt("total_price") : null,
                rs.getString("settlement_details"),
                rs.getString("message_content")
        ));
    }

    /**
     * 메시지 단건 조회 (JDBC)
     */
    public MessageDto findById(Long messageId) {
        String sql = "SELECT message_id, billing_id, user_id, status, scheduled_at, retry_count, ban_start_time, ban_end_time " +
                "FROM message WHERE message_id = ?";

        List<MessageDto> results = jdbcTemplate.query(sql, new Object[] { messageId }, (rs, rowNum) -> new MessageDto(
                rs.getLong("message_id"),
                rs.getLong("billing_id"),
                rs.getLong("user_id"),
                MessageStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("scheduled_at") != null
                        ? rs.getTimestamp("scheduled_at").toLocalDateTime()
                        : null,
                rs.getInt("retry_count"),
                rs.getTime("ban_start_time") != null
                        ? rs.getTime("ban_start_time").toLocalTime()
                        : null,
                rs.getTime("ban_end_time") != null
                        ? rs.getTime("ban_end_time").toLocalTime()
                        : null));

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
     * 실패 상태 Bulk 업데이트 (JDBC)
     * - 모든 실패 메시지를 동일한 scheduledAt으로 업데이트
     */
    public int bulkMarkFailed(List<Long> messageIds, LocalDateTime scheduledAt) {
        if (messageIds.isEmpty()) return 0;

        String placeholders = messageIds.stream()
                .map(id -> "?")
                .collect(Collectors.joining(","));

        String sql = "UPDATE message SET status = 'WAITED', retry_count = retry_count + 1, scheduled_at = ? " +
                "WHERE message_id IN (" + placeholders + ")";

        Object[] params = new Object[messageIds.size() + 1];
        params[0] = Timestamp.valueOf(scheduledAt);
        for (int i = 0; i < messageIds.size(); i++) {
            params[i + 1] = messageIds.get(i);
        }

        return jdbcTemplate.update(sql, params);
    }

    /**
     * 발송 연기 (JDBC)
     */
    public int defer(Long messageId, LocalDateTime scheduledAt) {
        String sql = "UPDATE message SET status = 'WAITED', scheduled_at = ? WHERE message_id = ?";
        return jdbcTemplate.update(sql, Timestamp.valueOf(scheduledAt), messageId);
    }

    /**
     * 발송 연기 Bulk (JDBC)
     */
    public int bulkDefer(List<Long> messageIds, LocalDateTime scheduledAt) {
        if (messageIds.isEmpty()) return 0;

        String placeholders = messageIds.stream()
                .map(id -> "?")
                .collect(Collectors.joining(","));

        String sql = "UPDATE message SET status = 'WAITED', scheduled_at = ? " +
                "WHERE message_id IN (" + placeholders + ")";

        Object[] params = new Object[messageIds.size() + 1];
        params[0] = Timestamp.valueOf(scheduledAt);
        for (int i = 0; i < messageIds.size(); i++) {
            params[i + 1] = messageIds.get(i);
        }

        return jdbcTemplate.update(sql, params);
    }

    /**
     * billingId 목록으로 메시지 조회 (INSERT 후 생성된 ID 조회용)
     */
    public List<MessageDto> findByBillingIds(List<Long> billingIds) {
        if (billingIds.isEmpty())
            return Collections.emptyList();

        String placeholders = billingIds.stream()
                .map(id -> "?")
                .collect(Collectors.joining(","));

        String sql = "SELECT message_id, billing_id, user_id, status, scheduled_at, retry_count, ban_start_time, ban_end_time " +
                "FROM message WHERE billing_id IN (" + placeholders + ")";

        return jdbcTemplate.query(sql, billingIds.toArray(), (rs, rowNum) -> new MessageDto(
                rs.getLong("message_id"),
                rs.getLong("billing_id"),
                rs.getLong("user_id"),
                MessageStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("scheduled_at") != null
                        ? rs.getTimestamp("scheduled_at").toLocalDateTime()
                        : null,
                rs.getInt("retry_count"),
                rs.getTime("ban_start_time") != null
                        ? rs.getTime("ban_start_time").toLocalTime()
                        : null,
                rs.getTime("ban_end_time") != null
                        ? rs.getTime("ban_end_time").toLocalTime()
                        : null));
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
            Integer retryCount,
            java.time.LocalTime banStartTime,
            java.time.LocalTime banEndTime) {
    }

    public record MessageWithSnapshotDto(
            Long messageId,
            Long billingId,
            Long userId,
            MessageStatus status,
            LocalDateTime scheduledAt,
            Integer retryCount,
            java.time.LocalTime banStartTime,
            java.time.LocalTime banEndTime,
            Long snapshotMessageId,
            Long snapshotBillingId,
            java.time.LocalDate settlementMonth,
            Long snapshotUserId,
            String userName,
            String userEmail,
            String userPhone,
            Integer totalPrice,
            String settlementDetails,
            String messageContent) {
    }
}
