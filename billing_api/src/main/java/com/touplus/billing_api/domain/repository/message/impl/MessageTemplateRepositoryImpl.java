package com.touplus.billing_api.domain.repository.message.impl;

import com.touplus.billing_api.domain.message.entity.MessageTemplate;
import com.touplus.billing_api.domain.message.enums.MessageType;
import com.touplus.billing_api.domain.repository.message.MessageTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MessageTemplateRepositoryImpl implements MessageTemplateRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<MessageTemplate> ROW_MAPPER =
            (rs, rowNum) -> new MessageTemplate(
                    rs.getLong("template_id"),
                    rs.getString("template_name"),
                    MessageType.valueOf(rs.getString("message_type")),
                    rs.getString("template_content"),
                    toLocalDateTime(rs, "created_at"),
                    toLocalDateTime(rs, "updated_at"),
                    toLocalDateTime(rs, "deleted_at")
            );

    private static LocalDateTime toLocalDateTime(ResultSet rs, String column)
            throws SQLException {
        java.sql.Timestamp ts = rs.getTimestamp(column);
        return ts != null ? ts.toLocalDateTime() : null;
    }

    // Create
    @Override
    public Long save(String templateName, MessageType messageType, String templateContent) {
        String sql = """
            INSERT INTO billing_message.message_template
            (template_name, message_type, template_content, created_at, updated_at)
            VALUES (?, ?, ?, NOW(), NOW())
        """;

        jdbcTemplate.update(sql, templateName, messageType.name(), templateContent);

        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    // Read
    @Override
    public Optional<MessageTemplate> findById(Long templateId) {
        String sql = """
            SELECT *
            FROM billing_message.message_template
            WHERE template_id = ?
              AND deleted_at IS NULL
        """;

        List<MessageTemplate> result = jdbcTemplate.query(sql, ROW_MAPPER, templateId);
        return result.stream().findFirst();
    }

    @Override
    public List<MessageTemplate> findAll() {
        String sql = """
            SELECT *
            FROM billing_message.message_template
            WHERE deleted_at IS NULL
            ORDER BY template_id DESC
        """;

        return jdbcTemplate.query(sql, ROW_MAPPER);
    }

    @Override
    public List<MessageTemplate> findByMessageType(MessageType messageType) {
        String sql = """
            SELECT *
            FROM billing_message.message_template
            WHERE message_type = ?
              AND deleted_at IS NULL
            ORDER BY template_id DESC
        """;

        return jdbcTemplate.query(sql, ROW_MAPPER, messageType.name());
    }

    // Update
    @Override
    public void update(Long templateId, String templateName, MessageType messageType, String templateContent) {
        String sql = """
            UPDATE billing_message.message_template
            SET template_name = ?,
                message_type = ?,
                template_content = ?,
                updated_at = NOW()
            WHERE template_id = ?
              AND deleted_at IS NULL
        """;

        jdbcTemplate.update(sql, templateName, messageType.name(), templateContent, templateId);
    }

    // Delete (soft delete)
    @Override
    public void delete(Long templateId) {
        String sql = """
            UPDATE billing_message.message_template
            SET deleted_at = NOW()
            WHERE template_id = ?
        """;

        jdbcTemplate.update(sql, templateId);
    }
}
