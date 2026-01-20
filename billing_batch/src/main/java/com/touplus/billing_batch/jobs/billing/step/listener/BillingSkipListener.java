package com.touplus.billing_batch.jobs.billing.step.listener;

import com.touplus.billing_batch.common.BillingException;
import com.touplus.billing_batch.common.BillingFatalException;
import com.touplus.billing_batch.domain.dto.BillingCalculationResult;
import com.touplus.billing_batch.domain.entity.BillingErrorLog;
import com.touplus.billing_batch.domain.entity.BillingUser;
import com.touplus.billing_batch.domain.enums.ErrorType;
import com.touplus.billing_batch.domain.repository.BillingErrorLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillingSkipListener implements SkipListener<BillingUser, BillingCalculationResult>, StepExecutionListener {

    private final BillingErrorLogRepository errorLogRepository;
    private StepExecution stepExecution;

    // Step 시작 시점에 StepExecution을 주입받음
    @Override
    public void beforeStep(StepExecution stepExecution) {
        this.stepExecution = stepExecution;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ExitStatus afterStep(StepExecution stepExecution){
        // step 성공 시 배치 작업으로 돌아가기
        if (stepExecution.getStatus() != BatchStatus.FAILED) {
            return stepExecution.getExitStatus();
        }

        // stepExecution에 들어있는 FAIL 예외 가져오기. 없으면 새로 생성. 있으면 가장 최신 것 조회.
        Throwable t = stepExecution.getFailureExceptions().isEmpty()
                ? new RuntimeException("UNKNOWN_STEP_FAILURE")
                : stepExecution.getFailureExceptions().get(0);

        Long userId = null;
        String errorCode = "SYSTEM_ERROR";
        ErrorType errorType = ErrorType.SYSTEM;

        if (t instanceof BillingFatalException bfe) {
            userId = bfe.getUserId();
            errorCode = bfe.getErrorCode();
            errorType = ErrorType.SYSTEM; // 로직 오류
        } else if (t instanceof BillingException be) {
            userId = be.getUserId();
            errorCode = be.getErrorCode();
            errorType = ErrorType.DATA;
        }

        String targetMonth = stepExecution.getJobParameters().getString("targetMonth");
        if (targetMonth == null) {
            throw new IllegalStateException("jobParameter targetMonth is required");
        }
        LocalDate settlementMonth = LocalDate.parse(targetMonth);

        BillingErrorLog errorLog = BillingErrorLog.builder()
                .jobExecutionId(stepExecution.getJobExecutionId())
                .stepExecutionId(stepExecution.getId())
                .jobName(stepExecution.getJobExecution().getJobInstance().getJobName())
                .stepName(stepExecution.getStepName() + ": STEP_FAILED")
                .userId(userId)
                .settlementMonth(settlementMonth)
                .errorType(errorType)
                .errorCode(errorCode)
                .errorMessage(t.getMessage())
                .isRecoverable(false)
                .processed(false)
                .build();

        errorLogRepository.save(errorLog);
        return stepExecution.getExitStatus();
    }

    @Override
    public void onSkipInRead(Throwable t) {
        log.error(">> [Skip In Read] Cause: {}", t.getMessage());
        // Read 단계는 대상 유저를 특정하기 어려우므로 필요 시 시스템 에러로 기록
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW) // 에러 로그는 메인 트랜잭션 롤백과 무관하게 저장
    public void onSkipInProcess(BillingUser item, Throwable t) {
        log.error(">> [Skip In Process] UserID: {}, Reason: {}", item.getUserId(), t.getMessage());
        saveErrorLog(item.getUserId(), t, "PROCESS");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onSkipInWrite(BillingCalculationResult item, Throwable t) {
        log.error(">> [Skip In Write] UserID: {}, Reason: {}", item.getUserId(), t.getMessage());
        saveErrorLog(item.getUserId(), t, "WRITE");
    }

    /**
     * DB에 에러 로그 저장
     */
    private void saveErrorLog(Long userId, Throwable t, String stepPhase) {
        String errorCode = (t instanceof BillingException be) ? be.getErrorCode() : "SYSTEM_ERROR";
        ErrorType errorType = determineErrorType(t);

        // 현재 Job의 파라미터에서 정산월을 가져오거나 현재 날짜 사용
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
                .isRecoverable(true)
                .processed(false)
                .build();

        errorLogRepository.save(errorLog);
    }

    private ErrorType determineErrorType(Throwable t) {
        if (t instanceof BillingException) {
            return ErrorType.DATA; // 비즈니스 로직 에러
        }
        return ErrorType.SYSTEM; // 예상치 못한 시스템 에러
    }
}