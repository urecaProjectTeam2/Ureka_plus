package com.touplus.billing_api.domain.repository.billing.impl;

import com.touplus.billing_api.admin.dto.BillingProductStatResponse;
import com.touplus.billing_api.domain.billing.entity.BillingProduct;
import com.touplus.billing_api.domain.billing.enums.ProductType;
import com.touplus.billing_api.domain.repository.billing.BillingProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
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
            FROM billing_batch.billing_product
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
            FROM billing_batch.billing_product
            WHERE product_id = :id
        """;

        return namedJdbcTemplate.queryForObject(
                sql,
                Map.of("id", id),
                this::mapRow
        );
    }

    /**
     * 특정 상품 타입을 가진 구독 상품들을 기준으로,
     * 사용자별 구독 수를 집계하여 count 내림차순으로 조회
     */
    @Override
    public List<BillingProductStatResponse> findTopSubscribedByProductType(
            List<String> productTypes, int limit
    ) {
        if (productTypes == null || productTypes.isEmpty()) {
            return List.of();
        }

        String sql = """
            SELECT 
                bp.product_id,
                bp.product_name,
                bp.product_type,
                bp.price,
                COUNT(usp.user_id) AS subscribe_count
            FROM billing_batch.billing_product bp
            LEFT JOIN billing_batch.user_subscribe_product usp
                ON bp.product_id = usp.product_id
                AND usp.deleted_at IS NULL
            WHERE bp.product_type IN (:productTypes)
            GROUP BY bp.product_id, bp.product_name, bp.product_type, bp.price
            ORDER BY subscribe_count DESC
            LIMIT :limit
        """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("productTypes", productTypes)
                .addValue("limit", limit);

        return namedJdbcTemplate.query(
                sql,
                params,
                (rs, rowNum) -> BillingProductStatResponse.builder()
                        .productId(rs.getLong("product_id"))
                        .productName(rs.getString("product_name"))
                        .productType(ProductType.valueOf(rs.getString("product_type")))
                        .price(rs.getInt("price"))
                        .subscribeCount(rs.getLong("subscribe_count"))
                        .build()
        );
    }
}
