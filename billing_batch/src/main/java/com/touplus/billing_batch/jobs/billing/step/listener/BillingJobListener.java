package com.touplus.billing_batch.jobs.billing.step.listener;

import java.util.Map;
import java.util.stream.Collectors;

import com.touplus.billing_batch.common.BillingFatalException;
import com.touplus.billing_batch.jobs.billing.cache.BillingReferenceCache;
import com.touplus.billing_batch.domain.dto.BillingDiscountDto;
import com.touplus.billing_batch.domain.dto.BillingProductDto;
import com.touplus.billing_batch.domain.repository.service.BillingDiscountRepository;
import com.touplus.billing_batch.domain.repository.service.BillingProductRepository;

import com.touplus.billing_batch.jobs.billing.cache.BillingReferenceLoader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillingJobListener implements JobExecutionListener {

    private final BillingErrorLogger errorLogger;
    private final BillingReferenceLoader billingReferenceLoader;

    @Override
    @Transactional(readOnly = true) // 마스터 데이터 대량 조회 성능 최적화
    public void beforeJob(JobExecution jobExecution) {
        try {
            billingReferenceLoader.loadOrThrow();
        } catch (Exception e) {
            errorLogger.saveForJobLevel(jobExecution, 0L, e, "BEFORE_JOB");
            throw e;
        }
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        // Job 종료 후 메모리 점유를 해제하기 위해 캐시를 비웁니다.
        log.info(">>> [AfterJob] 배치가 종료되어 캐시 데이터를 정리합니다.");
        billingReferenceLoader.clear();
    }
}