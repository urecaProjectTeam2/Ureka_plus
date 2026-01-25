package com.touplus.billing_api.domain.repository.message.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                    rs.getString("status"),
                    rs.getString("settlement_month"),
                    rs.getString("settlement_details")
            );
        }
    };

    // 전체 조회 - 페이징 처리 ㅇ
    @Override
    public List<MessageWithSettlementMonthDto> findAll(int page, int pageSize) {
        int offset = page * pageSize;
        String sql = """
            SELECT m.message_id,
                   m.billing_id,
                   m.status,
                   b.settlement_month,
                   b.settlement_details
            FROM billing_message.message m
            INNER JOIN billing_message.billing_snapshot b
                ON m.billing_id = b.billing_id
            ORDER BY m.message_id
            LIMIT ? OFFSET ?;
        """;
        return jdbcTemplate.query(sql, rowMapper, pageSize, offset);
    }
    
	// 전체 조회 - 페이징 처리 없이 전체 목록 전부 조회
	@Override
	public List<MessageWithSettlementMonthDto> findAll() {
	    String sql = """
	        SELECT m.message_id,
	               m.billing_id,
	               m.status,
	               b.settlement_month,
	               b.settlement_details
	        FROM billing_message.message m
	        INNER JOIN billing_message.billing_snapshot b
	            ON m.billing_id = b.billing_id
	        ORDER BY m.message_id;
	    """;
	    return jdbcTemplate.query(sql, rowMapper);
	}

    // 월별 조회
    @Override
    public List<MessageWithSettlementMonthDto> findBySettlementMonth(String settlementMonth, int page, int pageSize) {
        int offset = page * pageSize;
        String sql = """
            SELECT m.message_id,
                   m.billing_id,
                   m.status,
                   b.settlement_month,
                   b.settlement_details
            FROM billing_message.message m
            INNER JOIN billing_message.billing_snapshot b
                ON m.billing_id = b.billing_id
            WHERE b.settlement_month = ?
            ORDER BY m.message_id
            LIMIT ? OFFSET ?;
        """;
        return jdbcTemplate.query(sql, rowMapper, settlementMonth, pageSize, offset);
    }

    // 상태 조회
    @Override
    public List<MessageWithSettlementMonthDto> findAllByStatus(String messageStatus, int page, int pageSize) {
        int offset = page * pageSize;

        String sql = """
            SELECT m.message_id,
                   m.billing_id,
                   m.status,
                   b.settlement_month,
                   b.settlement_details
            FROM billing_message.message m
            INNER JOIN billing_message.billing_snapshot b
                ON m.billing_id = b.billing_id
            WHERE m.status = ?
            ORDER BY b.settlement_month DESC, m.message_id ASC
            LIMIT ? OFFSET ?
        """;

        return jdbcTemplate.query(sql, rowMapper, messageStatus, pageSize, offset);
    }

    // 메시지 개수 세기
    @Override
    public long countAll() {

        String sql = """
            SELECT COUNT(*)
            FROM billing_message.message
        """;

        return jdbcTemplate.queryForObject(sql, Long.class);
    }
    
    @Override
    public long countByStatus(String messageStatus) {
        String sql = "SELECT COUNT(*) FROM billing_message.message m WHERE m.status = ?";
        return jdbcTemplate.queryForObject(sql, Long.class, messageStatus);
    }

    // 통합 페이징 - 파라미터 없으면 전체 출력
    @Override
    public long countMessages(String messageStatus, String settlementMonth) {
        StringBuilder sql = new StringBuilder(
        		"SELECT COUNT(*) FROM billing_message.message m " +
        		        "INNER JOIN billing_message.billing_snapshot b ON m.billing_id = b.billing_id " +
        		        "WHERE 1=1"
        		        );
        List<Object> params = new ArrayList<>();
        if (messageStatus != null && !messageStatus.isEmpty()) {
            sql.append(" AND m.status = ?");
            params.add(messageStatus);
        }
        if (settlementMonth != null && !settlementMonth.isEmpty()) {
            sql.append(" AND b.settlement_month = ?");
            params.add(settlementMonth);
        }
        return jdbcTemplate.queryForObject(sql.toString(), params.toArray(), Long.class);
    }

    // 통합 조회(클릭시 다 따로 됨)
    @Override
    public List<MessageWithSettlementMonthDto> findMessages(String messageStatus, String settlementMonth, int page, int pageSize) {
        int offset = page * pageSize;

        StringBuilder sql = new StringBuilder("""
            SELECT
		        m.message_id,
		        m.billing_id,
		        m.status,
		        b.settlement_month,
		        b.settlement_details
		    FROM billing_message.message m
		    INNER JOIN billing_message.billing_snapshot b
		        ON m.billing_id = b.billing_id
		    WHERE 1=1
        """);

        List<Object> params = new ArrayList<>();
        if (messageStatus != null && !messageStatus.isEmpty()) {
            sql.append(" AND m.status = ?");
            params.add(messageStatus);
        }
        if (settlementMonth != null && !settlementMonth.isEmpty()) {
            sql.append(" AND b.settlement_month = ?");
            params.add(settlementMonth);
        }

        sql.append(" ORDER BY b.settlement_month DESC, m.message_id ASC LIMIT ? OFFSET ?");
        params.add(pageSize);
        params.add(offset);

        return jdbcTemplate.query(sql.toString(), rowMapper, params.toArray());
    }
    
    
    @Override
    public Map<String, Long> countGroupByStatus() {

        String sql = """
            SELECT status, COUNT(*) AS cnt
            FROM billing_message.message
            GROUP BY status
        """;

        return jdbcTemplate.query(sql, rs -> {
            Map<String, Long> result = new HashMap<>();
            while (rs.next()) {
                result.put(rs.getString("status"), rs.getLong("cnt"));
            }
            return result;
        });
    }

}

