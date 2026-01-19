package com.touplus.billing_batch.domain.repository;

import com.touplus.billing_batch.domain.entity.BillingUser;
import com.touplus.billing_batch.domain.entity.Unpaid;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class UnpaidRepository {

    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    /* ===============================
     * 공통 RowMapper
     * =============================== */
    private Unpaid mapRow(ResultSet rs, int rowNum) throws SQLException {
        return Unpaid.builder()
                .id(rs.getLong("unpaid_id"))
                .unpaidPrice(rs.getInt("unpaid_price"))
                .unpaidMonth(
                        rs.getDate("unpaid_month").toLocalDate()
                )
                .paid(rs.getBoolean("is_paid"))
                .user(
                        BillingUser.builder()
                                .userId(rs.getLong("user_id"))
                                .build()
                )
                .build();
    }

    /**
     * JPA: findByUser
     */
    public List<Unpaid> findByUser(BillingUser user) {
        String sql = """
            SELECT *
            FROM unpaid
            WHERE user_id = :userId
        """;

        MapSqlParameterSource params =
                new MapSqlParameterSource("userId", user.getUserId());

        return namedJdbcTemplate.query(sql, params, this::mapRow);
    }

    /**
     * JPA: findByPaidFalseAndUnpaidMonthBefore
     */
    public List<Unpaid> findByPaidFalseAndUnpaidMonthBefore(LocalDate month) {
        String sql = """
            SELECT *
            FROM unpaid
            WHERE is_paid = false
              AND unpaid_month < :month
        """;

        MapSqlParameterSource params =
                new MapSqlParameterSource("month", month);

        return namedJdbcTemplate.query(sql, params, this::mapRow);
    }

    /**
     * JPA:
     * findByUserIdIn AND paid = false
     */
    public List<Unpaid> findByUserIdIn(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }

        String sql = """
            SELECT *
            FROM unpaid
            WHERE user_id IN (:userIds)
              AND is_paid = false
        """;

        MapSqlParameterSource params =
                new MapSqlParameterSource("userIds", userIds);

        return namedJdbcTemplate.query(sql, params, this::mapRow);
    }
}
