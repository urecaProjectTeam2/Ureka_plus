package com.touplus.billing_batch.jobs.billing.step.listener;

import com.touplus.billing_batch.common.BillingException;
import com.touplus.billing_batch.common.BillingFatalException;
import com.touplus.billing_batch.domain.entity.BillingErrorLog;
import com.touplus.billing_batch.domain.enums.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class BillingStepListener implements StepExecutionListener {

    private final BillingErrorLogger billingErrorLogger;
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


        Long userId = (t instanceof BillingFatalException bfe) ? bfe.getUserId()
                : (t instanceof BillingException be) ? be.getUserId() : 0L;

        log.error(">> [Stop In Process] UserId: {}, Reason: {}", userId, t.getMessage());
        billingErrorLogger.saveErrorLog(stepExecution, userId, t, "PROCESSOR", false);

        return stepExecution.getExitStatus();
    }
}
