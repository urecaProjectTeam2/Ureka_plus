package com.touplus.billing_api.domain.repository.billing.impl;

import com.touplus.billing_api.domain.billing.entity.BillingProduct;
import com.touplus.billing_api.domain.billing.enums.ProductType;
import com.touplus.billing_api.domain.repository.billing.BillingProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class BillingProductRepositoryImpl implements BillingProductRepository {
    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    // 공통 RowMapper
    private BillingProduct mapRow(ResultSet rs, int rowNum) throws SQLException {
        return BillingProduct.builder()
                .productId(rs.getLong("product_id"))
                .productName(rs.getString("product_name"))
                .productType(ProductType.valueOf(rs.getString("product_type")))
                .price(rs.getInt("price"))
                .build();
    }

    /**
     * JPA: findAll()
     */
    @Override
    public List<BillingProduct> findAll() {
        String sql = """
            SELECT *
            FROM billing_product
        """;

        return namedJdbcTemplate.query(sql, this::mapRow);
    }

    /**
     * JPA: findById()
     */
    @Override
    public BillingProduct findById(Long id) {
        String sql = """
            SELECT *
            FROM billing_product
            WHERE product_id = :id
        """;

        return namedJdbcTemplate.queryForObject(
                sql,
                Map.of("id", id),
                this::mapRow
        );
    }
}
