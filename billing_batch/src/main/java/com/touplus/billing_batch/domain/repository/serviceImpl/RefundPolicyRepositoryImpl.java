package com.touplus.billing_batch.domain.repository.serviceImpl;

import com.touplus.billing_batch.domain.entity.RefundPolicy;
import com.touplus.billing_batch.domain.repository.service.RefundPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RefundPolicyRepositoryImpl implements RefundPolicyRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<RefundPolicy> rowMapper = (rs, rowNum) -> RefundPolicy.builder()
            .refundPolicyId(rs.getLong("refund_policy_id"))
            .productId(rs.getLong("product_id"))
            .refundDuration(rs.getInt("refund_duration"))
            .build();

    @Override
    public Optional<RefundPolicy> findByProductId(Long productId) {
        List<RefundPolicy> results = jdbcTemplate.query( "SELECT * FROM refund_policy WHERE product_id = ?", rowMapper, productId);
        return results.stream().findFirst();
    }

    @Override
    public List<RefundPolicy> findAll() {
        return jdbcTemplate.query("SELECT * FROM refund_policy", rowMapper);
    }
}
