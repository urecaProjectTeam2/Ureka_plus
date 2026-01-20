package com.touplus.billing_batch.domain.repository;

import com.touplus.billing_batch.domain.entity.BillingDiscount;
import com.touplus.billing_batch.domain.enums.DiscountType;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class BillingDiscountRepositoryImpl implements BillingDiscountRepository {

    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    // 공통 RowMapper
    private BillingDiscount mapRow(ResultSet rs, int rowNum) throws SQLException {
        return BillingDiscount.builder()
                .discountId(rs.getLong("discount_id"))
                .discountName(rs.getString("discount_name"))
                .isCash(DiscountType.valueOf(rs.getString("is_cash")))
                .cash(rs.getObject("cash", Integer.class))
                .percent(rs.getObject("percent", Double.class))
                .build();
    }

    /**
     * JPA: findAll()
     */
    @Override
    public List<BillingDiscount> findAll() {
        String sql = """
            SELECT *
            FROM billing_discount
        """;

        return namedJdbcTemplate.query(sql, this::mapRow);
    }

    /**
     * JPA: findById()
     */
    @Override
    public BillingDiscount findById(Long id) {
        String sql = """
            SELECT *
            FROM billing_discount
            WHERE discount_id = :id
        """;

        return namedJdbcTemplate.queryForObject(
                sql,
                Map.of("id", id),
                this::mapRow
        );
    }
}
