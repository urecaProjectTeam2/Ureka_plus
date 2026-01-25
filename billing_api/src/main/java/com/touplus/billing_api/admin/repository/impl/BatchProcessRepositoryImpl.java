package com.touplus.billing_api.admin.repository.impl;

import java.time.LocalDate;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.touplus.billing_api.admin.enums.ProcessType;
import com.touplus.billing_api.admin.repository.BatchProcessRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class BatchProcessRepositoryImpl implements BatchProcessRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public ProcessType findLatestJobStatus() {
        String sql = "SELECT job FROM billing_batch.batch_process";

        return jdbcTemplate.queryForObject(
            sql,
            (rs, rowNum) -> {
                String val = rs.getString("job");
                return val != null ? ProcessType.valueOf(val.trim().toUpperCase()) : ProcessType.WAITED;
            }
        );
    }

    @Override
    public ProcessType findLatestKafkaSentStatus() {
        String sql = "SELECT kafka_sent FROM billing_batch.batch_process";

        return jdbcTemplate.queryForObject(
            sql,
            (rs, rowNum) -> {
                String val = rs.getString("kafka_sent");
                return val != null ? ProcessType.valueOf(val.trim().toUpperCase()) : ProcessType.WAITED;
            }
        );
    }

    
    /* 나중에 업데이트문 필요하다고 하면 주기
    @Override
    public ProcessType updateKafkaReceive() {
    String sql = """
    		UPDATE  billing_message.message_process
    		SET kafka_receive = 'PENDING';
        """;

    	return jdbcTemplate.query(
            sql,
            (rs, rowNum) -> ProcessType.valueOf(rs.getString("kafka_sent"))
        )
        .stream()
        .findFirst()
        .orElse(null);    
    }*/
    
    
    @Override
    public long countBatch(LocalDate settlementMonth) {
        String sql = """
            SELECT COUNT(*)
            FROM billing_batch.billing_result
            WHERE settlement_month = ?
        """;
        return jdbcTemplate.queryForObject(sql, Long.class, settlementMonth);
    }

    @Override
    public long countKafkaSent(LocalDate settlementMonth) {
      /*  String sql = """
            SELECT COUNT(*)
            FROM billing_message.billing_snapshot;
            WHERE settlement_month = ?
              AND kafka_sent = 'DONE'
        """;
        return jdbcTemplate.queryForObject(sql, Long.class, settlementMonth);
    }*/
    	return 0;
    }

}
