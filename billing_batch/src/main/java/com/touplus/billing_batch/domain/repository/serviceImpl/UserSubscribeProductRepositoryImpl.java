package com.touplus.billing_batch.domain.repository.serviceImpl;

import com.touplus.billing_batch.domain.entity.UserSubscribeProduct;
import com.touplus.billing_batch.domain.repository.service.UserSubscribeProductRepository;
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
                        rs.getObject("deleted_at", LocalDate.class)
                )
                .userId(rs.getLong("user_id"))
                .productId(rs.getLong("product_id"))
                .build();
    }

    @Override
    public List<UserSubscribeProduct> findActiveByUserId(Long userId) {

        String sql = """
            SELECT
                user_subscribe_product_id,
                created_month,
                deleted_at,
                user_id,
                product_id
            FROM user_subscribe_product
            WHERE user_id = :userId
              AND deleted_at IS NULL
        """;

        MapSqlParameterSource params =
                new MapSqlParameterSource("userId", userId);

        return namedJdbcTemplate.query(sql, params, this::mapRow);
    }

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
            FROM user_subscribe_product
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
