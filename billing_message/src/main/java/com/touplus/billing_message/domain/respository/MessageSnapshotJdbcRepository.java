package com.touplus.billing_message.domain.respository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class MessageSnapshotJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 메시지 스냅샷 단건 조회 (JDBC)
     */
    public MessageSnapshotDto findById(Long messageId) {
        String sql = """
                    SELECT message_id, billing_id, settlement_month, user_id, user_name,
                           user_email, user_phone, total_price, settlement_details, message_content
                    FROM message_snapshot WHERE message_id = ?
                """;

        List<MessageSnapshotDto> results = jdbcTemplate.query(sql, (rs, rowNum) -> new MessageSnapshotDto(
                rs.getLong("message_id"),
                rs.getLong("billing_id"),
                rs.getObject("settlement_month", LocalDate.class),
                rs.getLong("user_id"),
                rs.getString("user_name"),
                rs.getString("user_email"),
                rs.getString("user_phone"),
                rs.getInt("total_price"),
                rs.getString("settlement_details"),
                rs.getString("message_content")), messageId);

        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * 메시지 스냅샷 DTO
     */
    public record MessageSnapshotDto(
            Long messageId,
            Long billingId,
            LocalDate settlementMonth,
            Long userId,
            String userName,
            String userEmail,
            String userPhone,
            Integer totalPrice,
            String settlementDetails,
            String messageContent) {
    }
}
