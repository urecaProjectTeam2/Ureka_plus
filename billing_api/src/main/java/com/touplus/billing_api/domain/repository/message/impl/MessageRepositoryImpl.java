package com.touplus.billing_api.domain.repository.message.impl;

import com.touplus.billing_api.domain.message.entity.Message;
import com.touplus.billing_api.domain.message.enums.MessageStatus;
import com.touplus.billing_api.domain.repository.message.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class MessageRepositoryImpl implements MessageRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final RowMapper<Message> MESSAGE_ROW_MAPPER = new RowMapper<>() {
        @Override
        public Message mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Message(
                    rs.getLong("message_id"),
                    rs.getLong("billing_id"),
                    rs.getLong("user_id"),
                    MessageStatus.valueOf(rs.getString("status")),
                    rs.getTimestamp("scheduled_at") != null
                            ? rs.getTimestamp("scheduled_at").toLocalDateTime()
                            : null,
                    rs.getInt("retry_count"),
                    rs.getTime("ban_end_time") != null
                            ? rs.getTime("ban_end_time").toLocalTime()
                            : null
            );
        }
    };

    @Override
    public List<Message> findByStatus(MessageStatus status, int limit) {
        String sql = """
            SELECT *
            FROM billing_message.message
            WHERE status = :status
            ORDER BY scheduled_at
            LIMIT :limit
        """;

        return jdbcTemplate.query(
                sql,
                Map.of(
                        "status", status.name(),
                        "limit", limit
                ),
                MESSAGE_ROW_MAPPER
        );
    }

    @Override
    public List<Message> findByMessageIds(List<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return List.of();
        }

        String sql = """
            SELECT *
            FROM billing_message.message
            WHERE message_id IN (:messageIds)
        """;

        return jdbcTemplate.query(
                sql,
                Map.of("messageIds", messageIds),
                MESSAGE_ROW_MAPPER
        );
    }

    @Override
    public List<Message> findByUserId(Long userId) {
        String sql = """
            SELECT *
            FROM billing_message.message
            WHERE user_id = :userId
            ORDER BY message_id DESC
        """;

        return jdbcTemplate.query(
                sql,
                Map.of("userId", userId),
                MESSAGE_ROW_MAPPER
        );
    }

    @Override
    public List<Message> findSendableMessages(int limit) {
        String sql = """
            SELECT *
            FROM billing_message.message
            WHERE status = 'WAITED'
              AND (scheduled_at IS NULL OR scheduled_at <= NOW())
            ORDER BY scheduled_at
            LIMIT :limit
        """;

        return jdbcTemplate.query(
                sql,
                Map.of("limit", limit),
                MESSAGE_ROW_MAPPER
        );
    }

    @Override
    public void updateStatus(Long messageId, MessageStatus status) {
        String sql = """
            UPDATE billing_message.message
            SET status = :status
            WHERE message_id = :messageId
        """;

        jdbcTemplate.update(
                sql,
                Map.of(
                        "status", status.name(),
                        "messageId", messageId
                )
        );
    }

    @Override
    public void increaseRetryCount(Long messageId) {
        String sql = """
            UPDATE billing_message.message
            SET retry_count = retry_count + 1
            WHERE message_id = :messageId
        """;

        jdbcTemplate.update(
                sql,
                Map.of("messageId", messageId)
        );
    }
}
