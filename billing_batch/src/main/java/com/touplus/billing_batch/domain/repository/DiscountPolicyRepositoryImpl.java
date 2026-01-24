package com.touplus.billing_batch.domain.repository;

import com.touplus.billing_batch.domain.entity.DiscountPolicy;
import com.touplus.billing_batch.domain.enums.CalOrderType;
import com.touplus.billing_batch.domain.enums.DiscountRangeType;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DiscountPolicyRepositoryImpl implements DiscountPolicyRepository{
    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    private DiscountPolicy mapRow(ResultSet rs, int rowNum) throws SQLException {
        return DiscountPolicy.builder()
                .discountRangeId(rs.getLong("discount_range_id"))
                .calOrder(CalOrderType.from(rs.getString("cal_order")))
                .discountRange(DiscountRangeType.from(rs.getString("discount_range")))
                .build();
    }

    @Override
    public Optional<DiscountPolicy> findByDiscountRangeId(Long discountRangeId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("discountRangeId", discountRangeId);

        String sql = """
            SELECT
                discount_range_id,
                cal_order,
                discount_range
            FROM discount_policy
            WHERE discount_range_id = :discountRangeId
        """;

        return namedJdbcTemplate.query(
                sql,
                params,
                rs -> rs.next() ? Optional.of(mapRow(rs, 0)) : Optional.empty()
        );
    }
}
