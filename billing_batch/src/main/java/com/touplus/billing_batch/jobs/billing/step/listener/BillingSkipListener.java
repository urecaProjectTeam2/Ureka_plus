package com.touplus.billing_batch.jobs.billing.step.listener;

import com.touplus.billing_batch.common.BillingException;
import com.touplus.billing_batch.domain.dto.BillingCalculationResult;
import com.touplus.billing_batch.domain.entity.BillingErrorLog;
import com.touplus.billing_batch.domain.entity.BillingUser;
import com.touplus.billing_batch.domain.enums.ErrorType;
import com.touplus.billing_batch.domain.repository.BillingErrorLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

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
        LocalDate settlementMonth = LocalDate.now().withDayOfMonth(1);

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