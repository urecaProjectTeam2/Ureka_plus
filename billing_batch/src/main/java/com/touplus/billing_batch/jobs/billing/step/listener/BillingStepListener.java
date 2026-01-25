package com.touplus.billing_batch.jobs.billing.step.listener;

import com.touplus.billing_batch.common.BillingException;
import com.touplus.billing_batch.common.BillingFatalException;
import com.touplus.billing_batch.common.BillingFileRedirectionLogger;
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
    private final BillingFileRedirectionLogger fileLogger;

    // Step 시작 시점에 StepExecution을 주입받음
    @Override
    public void beforeStep(StepExecution stepExecution) {
        this.stepExecution = stepExecution;
        fileLogger.write("--- [Step 시작] " + stepExecution.getStepName() + " ---");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ExitStatus afterStep(StepExecution stepExecution){
        // 스탭 종료 시 처리 통계 --> 파일에 리다이렉션
        fileLogger.write(String.format("--- [Step 종료] %s ---", stepExecution.getStepName()));
        fileLogger.write(String.format("- 최종 상태: %s", stepExecution.getStatus()));
        fileLogger.write(String.format("- 읽기 건수: %d", stepExecution.getReadCount()));
        fileLogger.write(String.format("- 쓰기 성공: %d", stepExecution.getWriteCount()));
        fileLogger.write(String.format("- 스킵 건수: %d", stepExecution.getSkipCount()));
        // step 성공 시 배치 작업으로 돌아가기
        if (stepExecution.getStatus() != BatchStatus.FAILED) {
            return stepExecution.getExitStatus();
        }

        // stepExecution에 들어있는 FAIL 예외 가져오기. 없으면 새로 생성. 있으면 가장 최신 것 조회.
        Throwable wrapper = stepExecution.getFailureExceptions().isEmpty()
                ? new RuntimeException("UNKNOWN_STEP_FAILURE")
                : stepExecution.getFailureExceptions().get(0);

        String phase = determinePhase(wrapper);

        Throwable t = (wrapper.getCause() != null) ? wrapper.getCause() : wrapper;

        Long userId = (t instanceof BillingFatalException bfe) ? bfe.getUserId()
                : (t instanceof BillingException be) ? be.getUserId() : 0L;

        fileLogger.write(String .format("[ERROR 발생] 단계: %s, 사용자 ID: %d, 메세지 %s",phase,userId,t.getMessage()));

        log.error(">> [Stop In {}] UserId: {}, Reason: {}", phase, userId, t.getMessage());
        billingErrorLogger.saveErrorLog(stepExecution, userId, t, phase, false);

        return stepExecution.getExitStatus();
    }

    private String determinePhase(Throwable wrapper) {
        if (wrapper instanceof org.springframework.batch.core.step.skip.NonSkippableReadException) {
            return "READER";
        } else if (wrapper instanceof org.springframework.batch.core.step.skip.NonSkippableProcessException) {
            return "PROCESSOR";
        } else if (wrapper instanceof org.springframework.batch.core.step.skip.NonSkippableWriteException) {
            return "WRITER";
        }
        return "UNKNOWN";
    }
}
