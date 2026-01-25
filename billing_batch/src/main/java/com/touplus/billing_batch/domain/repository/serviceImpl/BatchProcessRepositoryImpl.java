package com.touplus.billing_batch.domain.repository.serviceImpl;

import com.touplus.billing_batch.domain.entity.BatchProcess;
import com.touplus.billing_batch.domain.enums.JobType;
import com.touplus.billing_batch.domain.repository.service.BatchProcessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

@Repository
@RequiredArgsConstructor
public class BatchProcessRepositoryImpl implements BatchProcessRepository {

    private final JdbcTemplate jdbcTemplate;

    public BatchProcess mapRow(ResultSet rs, int rowNum) throws SQLException {
        return BatchProcess.builder()
                .batchProcessId(rs.getLong("batch_process_id"))
                .job(JobType.from(rs.getString("job")))
                .kafkaSent(JobType.from(rs.getString("kafka_sent")))
                .settlementMonth(
                        rs.getDate("settlement_month").toLocalDate()
                )
                .build();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public int updateJob(JobType job, LocalDate startDate, LocalDate endDate) {
        // DB 구조에 맞춰 settlement_month 컬럼을 사용하도록 수정
        String sql = "UPDATE batch_process SET job = ? WHERE settlement_month = ?";

        // startDate를 settlement_month 조건으로 사용 (전달받은 기간의 시작일을 기준일로 가정)
        return jdbcTemplate.update(sql, job.name(), startDate);
    }


    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW) // 새 트랜잭션 시작
    public int updateKafkaSent(JobType jobType, LocalDate startDate, LocalDate endDate) {
        // 1. DB 컬럼명에 맞춰 SQL 수정 (job_type -> job, start_date -> settlement_month)
        // 2. kafka_sent에 boolean 대신 jobType의 이름을 전달
        String sql = "UPDATE batch_process SET kafka_sent = ? " +
                "WHERE job = ? AND settlement_month = ?";

        // 로그 출력으로 파라미터 확인 (디버깅용)
        System.out.println(">>> Updating Kafka Sent Status to: " + jobType.name() + " for Month: " + startDate);

        // 인자 순서: kafka_sent 업데이트 값, WHERE 조건(job, settlement_month)
        return jdbcTemplate.update(sql,
                jobType.name(),
                jobType.name(),
                startDate);
    }


    @Transactional
    @Override
    public int insertBatchProcess(LocalDate settlementMonth) {
        String sql = """
            INSERT INTO batch_process (
                settlement_month,
                job,
                kafka_sent
            )
            VALUES (?, ?, ?)
        """;

        return jdbcTemplate.update(
                sql,
                settlementMonth,
                JobType.WAITED.name(),
                JobType.WAITED.name()
        );
    }

}
