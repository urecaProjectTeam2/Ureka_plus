package com.touplus.billing_message.domain.respository;

import com.touplus.billing_message.domain.entity.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class MessageJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public void batchInsert(List<Message> messages) {
        String sql = """
            INSERT INTO message
            (billing_id, user_id, status, scheduled_at, retry_count, ban_end_time)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        jdbcTemplate.batchUpdate(
            sql,
            messages,
            messages.size(),
            (ps, m) -> {
                ps.setLong(1, m.getBillingId());
                ps.setLong(2, m.getUserId());
                ps.setString(3, m.getStatus().name());
                // LocalDateTime -> Timestamp 변환 필요
                ps.setTimestamp(4, m.getScheduledAt() != null ? Timestamp.valueOf(m.getScheduledAt()) : null);
                ps.setInt(5, m.getRetryCount());
                // LocalTime -> Time 변환
                ps.setTime(6, m.getBanEndTime() != null ? Time.valueOf(m.getBanEndTime()) : null);
            }
        );
    }
}

