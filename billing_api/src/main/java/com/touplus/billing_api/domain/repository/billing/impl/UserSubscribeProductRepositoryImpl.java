package com.touplus.billing_api.domain.repository.billing.impl;

import com.touplus.billing_api.domain.billing.entity.UserSubscribeProduct;
import com.touplus.billing_api.domain.repository.billing.UserSubscribeProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class UserSubscribeProductRepositoryImpl
        implements UserSubscribeProductRepository {

    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    /* ===============================
     * 공통 RowMapper
     * =============================== */
    private UserSubscribeProduct mapRow(ResultSet rs, int rowNum) throws SQLException {
        return UserSubscribeProduct.builder()
                .userSubscribeProductId(
                        rs.getLong("user_subscribe_product_id")
                )
                .createdMonth(
                        rs.getObject("created_month", LocalDate.class)
                )
                .deletedAt(
                        rs.getObject("deleted_at", LocalDateTime.class)
                )
                .userId(rs.getLong("user_id"))
                .productId(rs.getLong("product_id"))
                .build();
    }

    /**
     * JPA:
     * findActiveByUserId(Long userId)
     */
    @Override
    public List<UserSubscribeProduct> findActiveByUserId(Long userId) {

        String sql = """
            SELECT
                user_subscribe_product_id,
                created_month,
                deleted_at,
                user_id,
                product_id
            FROM billing_batch.user_subscribe_product
            WHERE user_id = :userId
              AND deleted_at IS NULL
        """;

        MapSqlParameterSource params =
                new MapSqlParameterSource("userId", userId);

        return namedJdbcTemplate.query(sql, params, this::mapRow);
    }

    /**
     * JPA:
     * findByUserIdIn(List<Long> userIds)
     */
    @Override
    public List<UserSubscribeProduct> findByUserIdIn(List<Long> userIds, LocalDate startDate, LocalDate endDate) {

        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }

        String sql =  """
            SELECT
                user_subscribe_product_id,
                created_month,
                deleted_at,
                user_id,
                product_id
            FROM billing_batch.user_subscribe_product
            WHERE user_id IN (:userIds)
              AND created_month <= :endDate
              AND (deleted_at IS NULL OR deleted_at >= :startDate)
        """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userIds", userIds)
                .addValue("startDate", startDate)
                .addValue("endDate", endDate);

        return namedJdbcTemplate.query(sql, params, this::mapRow);
    }
}
