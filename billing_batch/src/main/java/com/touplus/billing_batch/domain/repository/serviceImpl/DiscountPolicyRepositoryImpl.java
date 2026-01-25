package com.touplus.billing_batch.domain.repository.serviceImpl;

import com.touplus.billing_batch.domain.entity.DiscountPolicy;
import com.touplus.billing_batch.domain.enums.CalOrderType;
import com.touplus.billing_batch.domain.enums.DiscountRangeType;
import com.touplus.billing_batch.domain.repository.service.DiscountPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class DiscountPolicyRepositoryImpl implements DiscountPolicyRepository {
    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    private DiscountPolicy mapRow(ResultSet rs, int rowNum) throws SQLException {
        return DiscountPolicy.builder()
                .discountPolicyId(rs.getLong("discount_policy_id"))
                .calOrder(CalOrderType.from(rs.getString("cal_order")))
                .discountRange(DiscountRangeType.from(rs.getString("discount_range")))
                .build();
    }

    @Override
    public List<DiscountPolicy> findAll() {
        String sql = """
            SELECT
                discount_policy_id,
                cal_order,
                discount_range
            FROM discount_policy
        """;

        return namedJdbcTemplate.query(
                sql,
                this::mapRow
        );
    }

}
