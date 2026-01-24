package com.touplus.billing_batch.domain.repository;

import com.touplus.billing_batch.domain.entity.MvnoDiscountPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class MvnoDiscountPolicyRepositoryImpl implements MvnoDiscountPolicyRepository{
    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    private MvnoDiscountPolicy mapRow(ResultSet rs, int rowNum) throws SQLException {
        return MvnoDiscountPolicy.builder()
                .mvnoDiscountPolicyId(rs.getLong("mvno_discount_policy_id"))
                .productId(rs.getLong("product_id"))
                .discountDuration(rs.getInt("discount_duration"))
                .discountAmount(rs.getInt("discount_amount"))
                .build();
    }

    @Override
    public List<MvnoDiscountPolicy> findByProductId(Long productId) {
        String sql = """
            SELECT
                mvno_discount_policy_id,
                product_id,
                discount_duration,
                discount_amount
            FROM mvno_discount_policy
            WHERE product_id = :productId
        """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("productId", productId);

        return namedJdbcTemplate.query(sql, params, this::mapRow);
    }
}
