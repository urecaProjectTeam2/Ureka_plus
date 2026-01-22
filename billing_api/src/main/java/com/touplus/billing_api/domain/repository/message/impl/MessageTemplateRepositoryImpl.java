package com.touplus.billing_api.domain.repository.message.impl;

import com.touplus.billing_api.domain.message.entity.MessageTemplate;
import com.touplus.billing_api.domain.message.enums.MessageType;
import com.touplus.billing_api.domain.repository.message.MessageTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MessageTemplateRepositoryImpl implements MessageTemplateRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final RowMapper<MessageTemplate> ROW_MAPPER =
            new RowMapper<>() {
                @Override
                public MessageTemplate mapRow(ResultSet rs, int rowNum) throws SQLException {
                    return new MessageTemplate(
                            rs.getLong("template_id"),
                            rs.getString("template_name"),
                            MessageType.valueOf(rs.getString("message_type")),
                            rs.getString("template_content"),
                            rs.getTimestamp("created_at").toLocalDateTime(),
                            rs.getTimestamp("updated_at").toLocalDateTime(),
                            rs.getTimestamp("deleted_at") != null
                                    ? rs.getTimestamp("deleted_at").toLocalDateTime()
                                    : null
                    );
                }
            };

    @Override
    public Long save(String templateName, MessageType messageType, String templateContent) {
        String sql = """
            INSERT INTO message_template
            (template_name, message_type, template_content, created_at, updated_at)
            VALUES (:templateName, :messageType, :templateContent, NOW(), NOW())
        """;

        jdbcTemplate.update(
                sql,
                Map.of(
                        "templateName", templateName,
                        "messageType", messageType.name(),
                        "templateContent", templateContent
                )
        );

        return jdbcTemplate.getJdbcTemplate()
                .queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    @Override
    public Optional<MessageTemplate> findById(Long templateId) {
        String sql = """
            SELECT *
            FROM message_template
            WHERE template_id = :templateId
              AND deleted_at IS NULL
        """;

        List<MessageTemplate> result = jdbcTemplate.query(
                sql,
                Map.of("templateId", templateId),
                ROW_MAPPER
        );

        return result.stream().findFirst();
    }

    @Override
    public List<MessageTemplate> findAll() {
        String sql = """
            SELECT *
            FROM message_template
            WHERE deleted_at IS NULL
            ORDER BY template_id DESC
        """;

        return jdbcTemplate.query(sql, ROW_MAPPER);
    }

    @Override
    public List<MessageTemplate> findByMessageType(MessageType messageType) {
        String sql = """
            SELECT *
            FROM message_template
            WHERE message_type = :messageType
              AND deleted_at IS NULL
            ORDER BY template_id DESC
        """;

        return jdbcTemplate.query(
                sql,
                Map.of("messageType", messageType.name()),
                ROW_MAPPER
        );
    }

    @Override
    public void update(Long templateId, String templateName, MessageType messageType, String templateContent) {
        String sql = """
            UPDATE message_template
            SET template_name = :templateName,
                message_type = :messageType,
                template_content = :templateContent,
                updated_at = NOW()
            WHERE template_id = :templateId
              AND deleted_at IS NULL
        """;

        jdbcTemplate.update(
                sql,
                Map.of(
                        "templateId", templateId,
                        "templateName", templateName,
                        "messageType", messageType.name(),
                        "templateContent", templateContent
                )
        );
    }

    @Override
    public void delete(Long templateId) {
        String sql = """
            UPDATE message_template
            SET deleted_at = NOW()
            WHERE template_id = :templateId
        """;

        jdbcTemplate.update(
                sql,
                Map.of("templateId", templateId)
        );
    }
}
