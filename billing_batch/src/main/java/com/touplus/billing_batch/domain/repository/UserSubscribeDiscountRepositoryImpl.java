package com.touplus.billing_batch.domain.repository;

import com.touplus.billing_batch.domain.entity.BillingDiscount;
import com.touplus.billing_batch.domain.entity.BillingProduct;
import com.touplus.billing_batch.domain.entity.BillingUser;
import com.touplus.billing_batch.domain.entity.UserSubscribeDiscount;
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
public class UserSubscribeDiscountRepositoryImpl implements UserSubscribeDiscountRepository {

    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    /* ===============================
     * 공통 RowMapper
     * =============================== */
    private UserSubscribeDiscount mapRow(ResultSet rs, int rowNum) throws SQLException {
        return UserSubscribeDiscount.builder()
                .udsId(rs.getLong("user_discount_subscribe_id"))
                .discountSubscribeMonth(
                        rs.getObject("discount_subscribe_month", LocalDate.class)
                )
                .billingUser(
                        BillingUser.builder()
                                .userId(rs.getLong("user_id"))
                                .build()
                )
                .billingDiscount(
                        BillingDiscount.builder()
                                .discountId(rs.getLong("discount_id"))
                                .build()
                )
                .billingProduct(
                        BillingProduct.builder()
                                .productId(rs.getLong("product_id"))
                                .build()
                )
                .build();
    }

    /**
     * JPA:
     * findByUserIdIn(List<Long> userIds)
     */
    public List<UserSubscribeDiscount> findByUserIdIn(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }

        String sql = """
            SELECT
                user_discount_subscribe_id,
                discount_subscribe_month,
                user_id,
                discount_id,
                product_id
            FROM user_subscribe_discount
            WHERE user_id IN (:userIds)
        """;

        MapSqlParameterSource params =
                new MapSqlParameterSource("userIds", userIds);

        return namedJdbcTemplate.query(sql, params, this::mapRow);
    }
}
