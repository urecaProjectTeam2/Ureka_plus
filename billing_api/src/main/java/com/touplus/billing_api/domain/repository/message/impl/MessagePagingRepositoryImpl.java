package com.touplus.billing_api.domain.repository.message.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.touplus.billing_api.admin.dto.MessageWithSettlementMonthDto;
import com.touplus.billing_api.domain.repository.message.MessagePagingRepository;

import lombok.RequiredArgsConstructor;


@Repository
@RequiredArgsConstructor
public class MessagePagingRepositoryImpl implements MessagePagingRepository{

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<MessageWithSettlementMonthDto> rowMapper = new RowMapper<>() {
        @Override
        public MessageWithSettlementMonthDto mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new MessageWithSettlementMonthDto(
                    rs.getLong("message_id"),
                    rs.getLong("billing_id"),
                    rs.getString("message_status"),
                    rs.getString("settlement_month"),
                    rs.getString("content")
            );
        }
    };

    // 전체 조회
    @Override
    public List<MessageWithSettlementMonthDto> findAll(int page, int pageSize) {
        int offset = page * pageSize;
        String sql = """
            SELECT m.id AS message_id,
                   m.billing_id,
                   m.message_status,
                   b.settlement_month,
                   m.content
            FROM message m
            INNER JOIN billing_snapshot b
                ON m.billing_id = b.billing_id
            ORDER BY m.id
            LIMIT ? OFFSET ?
        """;
        return jdbcTemplate.query(sql, rowMapper, pageSize, offset);
    }

    // 월별 조회
    @Override
    public List<MessageWithSettlementMonthDto> findBySettlementMonth(String settlementMonth, int page, int pageSize) {
        int offset = page * pageSize;
        String sql = """
            SELECT m.id AS message_id,
                   m.billing_id,
                   m.message_status,
                   b.settlement_month,
                   m.content
            FROM message m
            INNER JOIN billing_snapshot b
                ON m.billing_id = b.billing_id
            WHERE b.settlement_month = ?
            ORDER BY m.id
            LIMIT ? OFFSET ?
        """;
        return jdbcTemplate.query(sql, rowMapper, settlementMonth, pageSize, offset);
    }

    // 상태 조회
    @Override
    public List<MessageWithSettlementMonthDto> findAllByStatus(String messageStatus, int page, int pageSize) {
        int offset = page * pageSize;

        String sql = """
            SELECT m.id AS message_id,
                   m.billing_id,
                   m.message_status,
                   b.settlement_month,
                   m.content
            FROM message m
            INNER JOIN billing_snapshot b
                ON m.billing_id = b.billing_id
            WHERE m.message_status = ?
            ORDER BY b.settlement_month DESC, m.id ASC
            LIMIT ? OFFSET ?
        """;

        return jdbcTemplate.query(sql, rowMapper, messageStatus, pageSize, offset);
    }

    @Override
    public long countByStatus(String messageStatus) {
        String sql = "SELECT COUNT(*) FROM message m WHERE m.message_status = ?";
        return jdbcTemplate.queryForObject(sql, Long.class, messageStatus);
    }

    // 메시지 개수 세기
    @Override
    public long countMessages(String messageStatus, String settlementMonth) {
        StringBuilder sql = new StringBuilder(
            "SELECT COUNT(*) FROM message m INNER JOIN billing_snapshot b ON m.billing_id = b.billing_id WHERE 1=1"
        );
        List<Object> params = new ArrayList<>();
        if (messageStatus != null && !messageStatus.isEmpty()) {
            sql.append(" AND m.message_status = ?");
            params.add(messageStatus);
        }
        if (settlementMonth != null && !settlementMonth.isEmpty()) {
            sql.append(" AND b.settlement_month = ?");
            params.add(settlementMonth);
        }
        return jdbcTemplate.queryForObject(sql.toString(), params.toArray(), Long.class);
    }

    // 메시지 찾기?
    @Override
    public List<MessageWithSettlementMonthDto> findMessages(String messageStatus, String settlementMonth, int page, int pageSize) {
        int offset = page * pageSize;

        StringBuilder sql = new StringBuilder("""
            SELECT m.id AS message_id,
                   m.billing_id,
                   m.message_status,
                   b.settlement_month,
                   m.content
            FROM message m
            INNER JOIN billing_snapshot b
                ON m.billing_id = b.billing_id
            WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();
        if (messageStatus != null && !messageStatus.isEmpty()) {
            sql.append(" AND m.message_status = ?");
            params.add(messageStatus);
        }
        if (settlementMonth != null && !settlementMonth.isEmpty()) {
            sql.append(" AND b.settlement_month = ?");
            params.add(settlementMonth);
        }

        sql.append(" ORDER BY b.settlement_month DESC, m.id ASC LIMIT ? OFFSET ?");
        params.add(pageSize);
        params.add(offset);

        return jdbcTemplate.query(sql.toString(), rowMapper, params.toArray());
    }
}

