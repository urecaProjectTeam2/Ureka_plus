package com.touplus.billing_batch.jobs.billing.step.listener;

import com.touplus.billing_batch.common.BillingException;
import com.touplus.billing_batch.common.BillingFatalException;
import com.touplus.billing_batch.domain.entity.BillingErrorLog;
import com.touplus.billing_batch.domain.enums.ErrorType;
import com.touplus.billing_batch.domain.repository.BillingErrorLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingErrorLogger {

    private final BillingErrorLogRepository errorLogRepository;

    /**
     * DB에 에러 로그 저장
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveForJobLevel(JobExecution jobExecution, Long userId, Throwable t, String phase) {
        String targetMonthStr = jobExecution.getJobParameters().getString("targetMonth");
        LocalDate settlementMonth = (targetMonthStr != null) ? LocalDate.parse(targetMonthStr) : LocalDate.now();

        BillingErrorLog errorLog = BillingErrorLog.builder()
                .jobExecutionId(jobExecution.getId())
                .jobName(jobExecution.getJobInstance().getJobName())
                .stepName("SYSTEM:" + phase)
                .userId(userId)
                .settlementMonth(settlementMonth)
                .errorType(ErrorType.SYSTEM) // Job 수준 에러는 시스템 에러로 간주
                .errorCode("JOB_INIT_ERROR")
                .errorMessage(t.getMessage())
                .resolved(false)
                .processed(false)
                .occurredAt(LocalDateTime.now())
                .build();

        errorLogRepository.save(errorLog);
    }

    // step 레벨 에러
    @Transactional(propagation = Propagation.REQUIRES_NEW) // 에러 로그는 메인 트랜잭션 롤백과 무관하게 저장
    public void saveErrorLog(StepExecution stepExecution, Long userId, Throwable t, String stepPhase, boolean resolved) {
        String errorCode = (t instanceof BillingException be) ? be.getErrorCode()
                : (t instanceof BillingFatalException bfe) ? bfe.getErrorCode() : "SYSTEM_ERROR";
        ErrorType errorType = determineErrorType(t);

        // 현재 Job의 파라미터에서 정산월을 가져옴
        String targetMonth = stepExecution.getJobParameters().getString("targetMonth");
        if (targetMonth == null) {
            throw new IllegalStateException("jobParameter targetMonth is required");
        }
        LocalDate settlementMonth = LocalDate.parse(targetMonth);

        BillingErrorLog errorLog = BillingErrorLog.builder()
                .jobExecutionId(stepExecution.getJobExecutionId())
                .stepExecutionId(stepExecution.getId())
                .jobName(stepExecution.getJobExecution().getJobInstance().getJobName())
                .stepName(stepExecution.getStepName() + ":" + stepPhase)
                .userId(userId)
                .settlementMonth(settlementMonth)
                .errorType(errorType)
                .errorCode(errorCode)
                .errorMessage(t.getMessage())
                .resolved(resolved)
                .processed(false)
                .build();

        errorLogRepository.save(errorLog);
    }

    private ErrorType determineErrorType(Throwable t) {
        if (t instanceof BillingException) {
            return ErrorType.DATA; // 데이터 로직 에러
        }
        return ErrorType.SYSTEM; // 로직 에러 & 예상치 못한 시스템 에러
    }
}
