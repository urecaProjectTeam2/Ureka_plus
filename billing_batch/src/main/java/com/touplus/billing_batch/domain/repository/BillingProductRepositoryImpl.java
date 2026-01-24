package com.touplus.billing_batch.domain.repository;

import com.touplus.billing_batch.domain.entity.BillingProduct;
import com.touplus.billing_batch.domain.enums.Network_type;
import com.touplus.billing_batch.domain.enums.ProductType;
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
                .networkType(Network_type.valueOf(rs.getString("network_type")))
                .build();
    }


    @Override
    public List<BillingProduct> findAll() {
        String sql = """
            SELECT *
            FROM billing_product
        """;

        return namedJdbcTemplate.query(sql, this::mapRow);
    }

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

    /**
     * JPA: findByIdIn (배치 핵심)
     */
    @Override
    public List<BillingProduct> findByIdIn(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }

        String sql = """
            SELECT *
            FROM billing_product
            WHERE product_id IN (:productIds)
        """;

        return namedJdbcTemplate.query(
                sql,
                Map.of("productIds", productIds),
                this::mapRow
        );
    }
}
