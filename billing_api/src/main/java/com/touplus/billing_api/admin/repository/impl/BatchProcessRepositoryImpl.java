package com.touplus.billing_api.admin.repository.impl;

import java.util.Map;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.touplus.billing_api.admin.enums.ProcessType;
import com.touplus.billing_api.admin.repository.BatchProcessRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class BatchProcessRepositoryImpl implements BatchProcessRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public ProcessType findLatestJobStatus() {
        String sql = """
            SELECT job
            FROM billing_batch.batch_process
        """;

        return jdbcTemplate.query(
            sql,
            (rs, rowNum) -> ProcessType.valueOf(rs.getString("job"))
        )
        .stream()
        .findFirst()
        .orElse(null);
    }

    @Override
    public ProcessType findLatestKafkaSentStatus() {
    String sql = """
            SELECT kafka_sent
            FROM billing_batch.batch_process
        """;

    	return jdbcTemplate.query(
            sql,
            (rs, rowNum) -> ProcessType.valueOf(rs.getString("kafka_sent"))
        )
        .stream()
        .findFirst()
        .orElse(null);    
    }
}
