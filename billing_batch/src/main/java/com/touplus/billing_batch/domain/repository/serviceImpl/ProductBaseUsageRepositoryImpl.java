package com.touplus.billing_batch.domain.repository.serviceImpl;

import com.touplus.billing_batch.domain.dto.ProductBaseUsageDto;
import com.touplus.billing_batch.domain.entity.ProductBaseUsage;
import com.touplus.billing_batch.domain.enums.UseType;
import com.touplus.billing_batch.domain.repository.service.ProductBaseUsageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProductBaseUsageRepositoryImpl implements ProductBaseUsageRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<ProductBaseUsage> rowMapper = (rs, rowNum) -> ProductBaseUsage.builder()
            .productBaseUsageId(rs.getLong("product_base_usage_id"))
            .productId(rs.getLong("product_id"))
            .overusePolicyId(rs.getLong("overuse_policy_id"))
            .useType(UseType.valueOf(rs.getString("use_type")))
            .basicAmount(rs.getInt("basic_amount"))
            .build();

    @Override
    public List<ProductBaseUsage> findByProductId(Long productId) {
        return jdbcTemplate.query("SELECT * FROM product_base_usage WHERE product_id = ?", rowMapper, productId);
    }

    @Override
    public List<ProductBaseUsage> findAll() {
        return jdbcTemplate.query(
                "SELECT * FROM product_base_usage",
                rowMapper
        );
    }
}