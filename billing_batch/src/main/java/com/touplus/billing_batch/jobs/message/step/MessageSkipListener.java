package com.touplus.billing_batch.jobs.message.step;

import com.touplus.billing_batch.domain.dto.BillingResultDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageSkipListener implements SkipListener<BillingResultDto, BillingResultDto> {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void onSkipInRead(Throwable t) {
        log.error("메시지 읽기 중 스킵 발생: {}", t.getMessage());
    }

    @Override
    public void onSkipInProcess(BillingResultDto item, Throwable t) {
        log.error("메시지 프로세싱 중 스킵 발생 (User ID: {}): {}", item.getUserId(), t.getMessage());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW) // 로그 + 상태 업데이트를 한 트랜잭션으로 묶음
    public void onSkipInWrite(BillingResultDto item, Throwable t) {
        log.error("메시지 전송 실패로 인한 스킵 처리 (User ID: {}): {}", item.getUserId(), t.getMessage());

        try {
            // 1. 에러 로그 저장
            insertErrorLog(item, t);

            // 2. [추가] billing_result 테이블 상태를 FAIL로 업데이트
            updateStatusToFail(item.getId());

        } catch (Exception e) {
            log.error("에러 핸들링 중 치명적 오류 발생: {}", e.getMessage());
        }
    }

    private void updateStatusToFail(Long id) {
        String sql = "UPDATE billing_result SET send_status = 'FAIL', processed_at = NOW() WHERE billing_result_id = ?";
        jdbcTemplate.update(sql, id);
    }

    private void insertErrorLog(BillingResultDto item, Throwable t) {
        // 기존 insert 로직과 동일 (생략 가능하나 유지를 위해 기록)
        StepExecution stepExecution = Optional.ofNullable(StepSynchronizationManager.getContext())
                .map(StepContext::getStepExecution)
                .orElse(null);

        String jobName = (stepExecution != null) ? stepExecution.getJobExecution().getJobInstance().getJobName() : "messageJob";
        String stepName = (stepExecution != null) ? stepExecution.getStepName() : "messageStep";
        Long jobExecutionId = (item.getBatchExecutionId() != null) ? item.getBatchExecutionId() :
                (stepExecution != null ? stepExecution.getJobExecution().getId() : 0L);

        String sql = "INSERT INTO batch_billing_error_log (" +
                "job_execution_id, step_execution_id, job_name, step_name, " +
                "settlement_month, user_id, error_type, error_message, " +
                "resolved, processed, occurred_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";

        jdbcTemplate.update(sql,
                jobExecutionId,                          // job_execution_id
                stepExecution != null ? stepExecution.getId() : null, // step_execution_id
                jobName,                                 // job_name
                stepName,                                // step_name
                item.getSettlementMonth(),               // settlement_month
                item.getUserId(),                        // user_id
                "NETWORK",                               // error_type (ENUM: 'NETWORK')
                truncateErrorMessage(t.getMessage()),    // error_message
                0,                                       // resolved (tinyint: 0)
                0                                        // processed (tinyint: 0)
        );
    }

    private String truncateErrorMessage(String message) {
        if (message == null) return "Unknown Network Error";
        return message.length() > 500 ? message.substring(0, 497) + "..." : message;
    }
}