package com.touplus.billing_batch.domain.repository.serviceImpl;

import com.touplus.billing_batch.domain.entity.OverusePolicy;
import com.touplus.billing_batch.domain.enums.UseType;
import com.touplus.billing_batch.domain.repository.service.OverusePolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OverusePolicyRepositoryImpl implements OverusePolicyRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<OverusePolicy> rowMapper = (rs, rowNum) -> OverusePolicy.builder()
            .overusePolicyId(rs.getLong("overuse_policy_id"))
            .useType(UseType.valueOf(rs.getString("use_type")))
            .unitPrice(rs.getDouble("unit_price"))
            .build();

    @Override
    public Optional<OverusePolicy> findById(Long id) {
        List<OverusePolicy> results = jdbcTemplate.query("SELECT * FROM overuse_policy WHERE overuse_policy_id = ?", rowMapper, id);
        return results.stream().findFirst();
    }

    @Override
    public List<OverusePolicy> findAll() {
        return jdbcTemplate.query("SELECT * FROM overuse_policy", rowMapper);
    }
}