package com.touplus.billing_batch.domain.repository;

import com.touplus.billing_batch.domain.entity.BillingErrorLog;
import com.touplus.billing_batch.domain.enums.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class BillingErrorLogRepositoryImpl
        implements BillingErrorLogRepository {

    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    /* ===============================
     * 공통 RowMapper
     * =============================== */
    private BillingErrorLog mapRow(ResultSet rs, int rowNum) throws SQLException {
        return BillingErrorLog.builder()
                .id(rs.getLong("error_log_id"))
                .jobExecutionId(rs.getObject("job_execution_id", Long.class))
                .stepExecutionId(rs.getObject("step_execution_id", Long.class))
                .jobName(rs.getString("job_name"))
                .stepName(rs.getString("step_name"))
                .settlementMonth(
                        rs.getObject("settlement_month", LocalDate.class)
                )
                .userId(rs.getObject("user_id", Long.class))
                .errorType(
                        ErrorType.valueOf(rs.getString("error_type"))
                )
                .errorCode(rs.getString("error_code"))
                .errorMessage(rs.getString("error_message"))
                .resolved(rs.getBoolean("resolved"))
                .processed(rs.getBoolean("processed"))
                .occurredAt(
                        rs.getObject("occurred_at", LocalDateTime.class)
                )
                .build();
    }

    /**
     * INSERT
     */
    @Override
    public void save(BillingErrorLog errorLog) {

        String sql = """
            INSERT INTO batch_billing_error_log (
                job_execution_id,
                step_execution_id,
                job_name,
                step_name,
                settlement_month,
                user_id,
                error_type,
                error_code,
                error_message,
                resolved,
                processed,
                occurred_at
            ) VALUES (
                :jobExecutionId,
                :stepExecutionId,
                :jobName,
                :stepName,
                :settlementMonth,
                :userId,
                :errorType,
                :errorCode,
                :errorMessage,
                :resolved,
                :processed,
                :occurredAt
            )
        """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("jobExecutionId", errorLog.getJobExecutionId())
                .addValue("stepExecutionId", errorLog.getStepExecutionId())
                .addValue("jobName", errorLog.getJobName())
                .addValue("stepName", errorLog.getStepName())
                .addValue("settlementMonth", errorLog.getSettlementMonth())
                .addValue("userId", errorLog.getUserId())
                .addValue("errorType", errorLog.getErrorType().name())
                .addValue("errorCode", errorLog.getErrorCode())
                .addValue("errorMessage", errorLog.getErrorMessage())
                .addValue("resolved", errorLog.isResolved())
                .addValue("processed", errorLog.isProcessed())
                .addValue("occurredAt", errorLog.getOccurredAt());

        namedJdbcTemplate.update(sql, params);
    }

    /**
     * 미처리 에러 전체 조회
     */
    @Override
    public List<BillingErrorLog> findUnprocessed() {

        String sql = """
            SELECT *
            FROM batch_billing_error_log
            WHERE processed = false
            ORDER BY occurred_at ASC
        """;

        return namedJdbcTemplate.query(sql, this::mapRow);
    }

    /**
     * 정산월 기준 미처리 에러 조회
     */
    @Override
    public List<BillingErrorLog> findUnprocessedBySettlementMonth(LocalDate settlementMonth) {

        String sql = """
            SELECT *
            FROM batch_billing_error_log
            WHERE processed = false
              AND settlement_month = :settlementMonth
            ORDER BY occurred_at ASC
        """;

        MapSqlParameterSource params =
                new MapSqlParameterSource("settlementMonth", settlementMonth);

        return namedJdbcTemplate.query(sql, params, this::mapRow);
    }

    /**
     * 처리 완료 처리
     */
    @Override
    public void markAsProcessed(Long errorLogId) {

        String sql = """
            UPDATE batch_billing_error_log
            SET processed = true
            WHERE error_log_id = :errorLogId
        """;

        MapSqlParameterSource params =
                new MapSqlParameterSource("errorLogId", errorLogId);

        namedJdbcTemplate.update(sql, params);
    }
}
