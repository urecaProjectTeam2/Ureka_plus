package com.touplus.billing_batch.jobs.billing.step.listener;

import com.touplus.billing_batch.common.BillingException;
import com.touplus.billing_batch.common.BillingFatalException;
import com.touplus.billing_batch.domain.dto.BillingCalculationResult;
import com.touplus.billing_batch.domain.dto.BillingUserBillingInfoDto;
import com.touplus.billing_batch.domain.entity.BillingErrorLog;
import com.touplus.billing_batch.domain.entity.BillingResult;
import com.touplus.billing_batch.domain.entity.BillingUser;
import com.touplus.billing_batch.domain.enums.ErrorType;
import com.touplus.billing_batch.domain.repository.BillingErrorLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;

@Slf4j
@StepScope
@Component
@RequiredArgsConstructor
public class BillingSkipListener implements SkipListener<BillingUserBillingInfoDto, BillingResult> {

    private final BillingErrorLogger billingErrorLogger;

    @Value("#{stepExecution}")
    private StepExecution stepExecution;

    @Override
    public void onSkipInRead(Throwable t) {
        log.error(">> [Skip In Read] Cause: {}", t.getMessage());
        billingErrorLogger.saveErrorLog(stepExecution, 0L, t, "READER", false);
        // Read 단계는 대상 유저를 특정하기 어려우므로 필요 시 시스템 에러로 기록
    }

    @Override
    public void onSkipInProcess(BillingUserBillingInfoDto item, Throwable t) {
        log.error(">> [Skip In Process] UserID: {}, Reason: {}", item.getUserId(), t.getMessage());
        billingErrorLogger.saveErrorLog(stepExecution, item.getUserId(), t, "PROCESSOR", false);
    }

    @Override
    public void onSkipInWrite(BillingResult item, Throwable t) {
        log.error(">> [Skip In Write] UserID: {}, Reason: {}", item.getUserId(), t.getMessage());
        billingErrorLogger.saveErrorLog(stepExecution, item.getUserId(), t, "WRITER", false);
    }


}