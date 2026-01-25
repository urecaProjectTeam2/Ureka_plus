package com.touplus.billing_api.domain.repository.billing.impl;


import com.touplus.billing_api.domain.billing.entity.Unpaid;
import com.touplus.billing_api.domain.repository.billing.UnpaidRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class UnpaidRepositoryImpl implements UnpaidRepository {
    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    private static final RowMapper<Unpaid> UNPAID_ROW_MAPPER =
            (rs, rowNum) -> Unpaid.builder()
                    .id(rs.getLong("unpaid_id"))
                    .userId(rs.getLong("user_id"))
                    .unpaidPrice(rs.getInt("unpaid_price"))
                    .unpaidMonth(rs.getObject("unpaid_month", LocalDate.class))
                    .paid(rs.getBoolean("is_paid"))
                    .build();

    @Override
    public List<Unpaid> findUnpaidUsers(int page, int size) {
        String sql = """
            SELECT unpaid_id,
                   user_id,
                   unpaid_price,
                   unpaid_month,
                   is_paid
            FROM billing_batch.unpaid
            WHERE is_paid = false
            ORDER BY unpaid_month DESC
            LIMIT :size OFFSET :offset
        """;

        int offset = (page - 1) * size;

        return namedJdbcTemplate.query(
                sql,
                Map.of("size", size, "offset", offset),
                UNPAID_ROW_MAPPER
        );
    }

    @Override
    public List<Unpaid> findUnpaidUsersByMonth(int page, int size, String month) {
        String sql = """
            SELECT unpaid_id,
                   user_id,
                   unpaid_price,
                   unpaid_month,
                   is_paid
            FROM billing_batch.unpaid
            WHERE is_paid = false
              AND DATE_FORMAT(unpaid_month, '%Y-%m') = :monthStr
            ORDER BY unpaid_month DESC
            LIMIT :size OFFSET :offset
        """;

        int offset = (page - 1) * size;
       // month yyyy-MM

        return namedJdbcTemplate.query(
                sql,
                Map.of(
                        "size", size,
                        "offset", offset,
                        "monthStr", month
                ),
                UNPAID_ROW_MAPPER
        );
    }

    @Override
    public List<Unpaid> searchUnpaidUsersByKeyword(int page, int size, String keyword) {
        StringBuilder sql = new StringBuilder("""
            SELECT u.unpaid_id,
                   u.user_id,
                   u.unpaid_price,
                   u.unpaid_month,
                   u.is_paid
            FROM billing_batch.unpaid u
            JOIN billing_message.users usr ON u.user_id = usr.user_id
            WHERE u.is_paid = false
        """);

        Map<String, Object> params = new java.util.HashMap<>();
        params.put("size", size);
        params.put("offset", (page - 1) * size);

        if (keyword != null && !keyword.isBlank()) {
            // 띄어쓰기 기준으로 키워드 분리
            String[] keywords = keyword.trim().split("\\s+");

            for (int i = 0; i < keywords.length; i++) {
                String key = keywords[i];
                sql.append(" AND (")
                        .append("usr.name LIKE :key").append(i)
                        .append(" OR usr.email LIKE :key").append(i)
                        .append(" OR usr.phone LIKE :key").append(i)
                        .append(" OR DATE_FORMAT(u.unpaid_month, '%Y-%m') LIKE :key").append(i)
                        .append(") ");
                params.put("key" + i, "%" + key + "%");
            }
        }

        sql.append(" ORDER BY u.unpaid_month DESC ");
        sql.append(" LIMIT :size OFFSET :offset ");

        return namedJdbcTemplate.query(sql.toString(), params, UNPAID_ROW_MAPPER);
    }

    @Override
    public long countUnpaidUsers() {
        String sql = """
        SELECT COUNT(*)
        FROM billing_batch.unpaid
        WHERE is_paid = false
    """;

        return namedJdbcTemplate.queryForObject(sql, Map.of(), Long.class);
    }

    @Override
    public long countUnpaidUsersByKeyword(String keyword) {

        StringBuilder sql = new StringBuilder("""
        SELECT COUNT(*)
        FROM billing_batch.unpaid u
        JOIN billing_message.users usr ON u.user_id = usr.user_id
        WHERE u.is_paid = false
    """);

        Map<String, Object> params = new java.util.HashMap<>();

        if (keyword != null && !keyword.isBlank()) {
            String[] keywords = keyword.trim().split("\\s+");

            for (int i = 0; i < keywords.length; i++) {
                sql.append(" AND (")
                        .append("usr.name LIKE :key").append(i)
                        .append(" OR usr.email LIKE :key").append(i)
                        .append(" OR usr.phone LIKE :key").append(i)
                        .append(" OR DATE_FORMAT(u.unpaid_month, '%Y-%m') LIKE :key").append(i)
                        .append(") ");
                params.put("key" + i, "%" + keywords[i] + "%");
            }
        }

        return namedJdbcTemplate.queryForObject(
                sql.toString(),
                params,
                Long.class
        );
    }
}
