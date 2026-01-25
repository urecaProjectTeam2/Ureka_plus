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
        String sql = "SELECT job FROM billing_batch.batch_process ORDER BY settlement_month ASC LIMIT 1";

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
        String sql = "SELECT kafka_sent FROM billing_batch.batch_process ORDER BY settlement_month ASC LIMIT 1"; 

        return jdbcTemplate.queryForObject(
            sql,
            (rs, rowNum) -> {
                String val = rs.getString("kafka_sent");
                return val != null ? ProcessType.valueOf(val.trim().toUpperCase()) : ProcessType.WAITED;
            }
        );
    }
    
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
