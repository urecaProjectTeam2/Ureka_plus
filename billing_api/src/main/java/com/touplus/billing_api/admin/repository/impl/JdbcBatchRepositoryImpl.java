package com.touplus.billing_api.admin.repository.impl;

import com.touplus.billing_api.admin.dto.BatchBillingErrorLogDto;
import com.touplus.billing_api.admin.dto.BatchHistoryDto;
import com.touplus.billing_api.admin.dto.PartitionStatusDto;
import com.touplus.billing_api.admin.repository.JdbcBatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class JdbcBatchRepositoryImpl implements JdbcBatchRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public List<BatchHistoryDto> findAllHistory() {
        // billing_result 조인을 제거하여 속도를 극대화함
        String sql = """
            SELECT
                 je.JOB_EXECUTION_ID,
                 je.STATUS,
                 je.START_TIME,
                 je.END_TIME,
                 je.EXIT_CODE,
                 TIMESTAMPDIFF(SECOND, je.START_TIME, je.END_TIME) as DURATION,
                 SUM(CASE WHEN se.STEP_NAME IN ('masterStep', 'messageJobStep')
                       THEN (se.READ_SKIP_COUNT + se.PROCESS_SKIP_COUNT + se.WRITE_SKIP_COUNT)\s
                       ELSE 0 END) as SKIP_COUNT,
                 SUM(CASE WHEN se.STEP_NAME NOT IN ('masterStep') THEN se.WRITE_COUNT ELSE 0 END) as TOTAL_WRITE
             FROM billing_batch.BATCH_JOB_EXECUTION je
             JOIN billing_batch.BATCH_STEP_EXECUTION se ON je.JOB_EXECUTION_ID = se.JOB_EXECUTION_ID
             GROUP BY je.JOB_EXECUTION_ID
             ORDER BY je.START_TIME DESC
             LIMIT 10
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> BatchHistoryDto.builder()
                .executionId(rs.getLong("JOB_EXECUTION_ID"))
                .status(rs.getString("STATUS"))
                .startTime(rs.getTimestamp("START_TIME").toLocalDateTime())
                .endTime(rs.getTimestamp("END_TIME") != null ? rs.getTimestamp("END_TIME").toLocalDateTime() : null)
                .duration(rs.getLong("DURATION"))
                .totalWrite(rs.getLong("TOTAL_WRITE"))
                .skipCount(rs.getLong("SKIP_COUNT"))
                .exitCode(rs.getString("EXIT_CODE"))
                .build());
    }

    @Override
    public List<PartitionStatusDto> findPartitionDetails(Long executionId) {
        String sql = """
            SELECT 
                STEP_NAME, 
                STATUS, 
                READ_COUNT, 
                WRITE_COUNT, 
                COMMIT_COUNT,
                (READ_SKIP_COUNT + PROCESS_SKIP_COUNT + WRITE_SKIP_COUNT) as SKIP_COUNT
            FROM billing_batch.BATCH_STEP_EXECUTION 
            WHERE JOB_EXECUTION_ID = :id
            AND STEP_NAME not in ('masterStep','createTopicStep')
            ORDER BY STEP_NAME ASC
            """;

        return jdbcTemplate.query(sql, Map.of("id", executionId), (rs, rowNum) -> PartitionStatusDto.builder()
                .stepName(rs.getString("STEP_NAME"))
                .status(rs.getString("STATUS"))
                .readCount(rs.getLong("READ_COUNT"))
                .writeCount(rs.getLong("WRITE_COUNT"))
                .commitCount(rs.getLong("COMMIT_COUNT"))
                .skipCount(rs.getLong("SKIP_COUNT"))
                .build());
    }

    // 비활성 상태일 때 보여줄 마지막 성공 배치 정보
    public Map<String, Object> findLatestSuccessfulBatch() {
        String sql = """
            SELECT END_TIME, JOB_EXECUTION_ID 
            FROM billing_batch.BATCH_JOB_EXECUTION 
            WHERE STATUS = 'COMPLETED' 
            ORDER BY END_TIME DESC LIMIT 1
            """;
        try {
            return jdbcTemplate.queryForMap(sql, Map.of());
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Long findLatestActiveExecutionId() {
        // STARTING 혹은 STARTED 상태인 가장 최근의 ID 하나를 가져옵니다.
        String sql = """
        SELECT JOB_EXECUTION_ID
        FROM billing_batch.BATCH_JOB_EXECUTION
        WHERE STATUS IN ('STARTING', 'STARTED')
        ORDER BY JOB_EXECUTION_ID DESC
        LIMIT 1
    """;
        try {
            return jdbcTemplate.queryForObject(sql, Map.of(), Long.class);
        } catch (EmptyResultDataAccessException e) {
            return null; // 현재 돌고 있는 배치가 없으면 null 반환
        }
    }

    // 최근 발생한 에러 로그 조회
    public List<BatchBillingErrorLogDto> findAllErrorLogsByJobId(Long jobExecutionId) {
        String sql = """
        SELECT 
            error_log_id, step_name, settlement_month, user_id, 
            error_type, error_code, error_message, occurred_at
        FROM billing_batch.batch_billing_error_log 
        WHERE job_execution_id = :jobId
        ORDER BY occurred_at DESC
    """;

        return jdbcTemplate.query(sql, Map.of("jobId", jobExecutionId), (rs, rowNum) ->
                BatchBillingErrorLogDto.builder()
                        .errorLogId(rs.getLong("error_log_id"))
                        .stepName(rs.getString("step_name"))
                        .settlementMonth(rs.getDate("settlement_month").toLocalDate())
                        .userId(rs.getLong("user_id"))
                        .errorType(rs.getString("error_type"))
                        .errorCode(rs.getString("error_code"))
                        .errorMessage(rs.getString("error_message"))
                        .occurredAt(rs.getTimestamp("occurred_at").toLocalDateTime())
                        .build()
        );
    }


    @Override
    public List<BatchBillingErrorLogDto> findErrorLogsByJobIdAndUserId(Long jobId, Long userId) {
        // [최적화] job_execution_id와 user_id 복합 인덱스를 활용한 고속 조회
        String sql = """
        SELECT 
            error_log_id, step_name, settlement_month, user_id, 
            error_type, error_code, error_message, occurred_at
        FROM billing_batch.batch_billing_error_log 
        WHERE job_execution_id = :jobId 
          AND user_id = :userId
        ORDER BY occurred_at DESC
    """;

        Map<String, Object> params = Map.of(
                "jobId", jobId,
                "userId", userId
        );

        return jdbcTemplate.query(sql, params, (rs, rowNum) ->
                BatchBillingErrorLogDto.builder()
                        .errorLogId(rs.getLong("error_log_id"))
                        .stepName(rs.getString("step_name"))
                        .settlementMonth(rs.getDate("settlement_month").toLocalDate())
                        .userId(rs.getLong("user_id"))
                        .errorType(rs.getString("error_type")) // String으로 처리
                        .errorCode(rs.getString("error_code"))
                        .errorMessage(rs.getString("error_message"))
                        .occurredAt(rs.getTimestamp("occurred_at").toLocalDateTime())
                        .build()
        );
    }
}