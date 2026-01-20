package com.touplus.billing_batch.common.listener;

import java.util.Map;
import java.util.stream.Collectors;

import com.touplus.billing_batch.common.BillingFatalException;
import com.touplus.billing_batch.common.BillingReferenceCache;
import com.touplus.billing_batch.domain.dto.BillingDiscountDto;
import com.touplus.billing_batch.domain.dto.BillingProductDto;
import com.touplus.billing_batch.domain.repository.BillingDiscountRepository;
import com.touplus.billing_batch.domain.repository.BillingProductRepository;

import com.touplus.billing_batch.jobs.billing.step.listener.BillingErrorLogger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillingJobListener implements JobExecutionListener {

    private final BillingReferenceCache referenceCache;
    private final BillingProductRepository billingProductRepository;
    private final BillingDiscountRepository billingDiscountRepository;
    private final BillingErrorLogger errorLogger;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        try {
            // 캐시 초기화
            referenceCache.clear();

            // 상품 캐싱
            Map<Long, BillingProductDto> productMap = billingProductRepository.findAll()
                    .stream()
                    .map(BillingProductDto::fromEntity)
                    .collect(Collectors.toMap(BillingProductDto::getProductId, p -> p));

            if (productMap.isEmpty()) {
                throw BillingFatalException.cacheNotFound("상품 마스터 데이터가 존재하지 않습니다.");
            }
            referenceCache.getProductMap().putAll(productMap);

            // 할인 캐싱
            Map<Long, BillingDiscountDto> discountMap = billingDiscountRepository.findAll()
                    .stream()
                    .map(BillingDiscountDto::fromEntity)
                    .collect(Collectors.toMap(BillingDiscountDto::getDiscountId, d -> d));

            if (discountMap.isEmpty()) {
                throw BillingFatalException.cacheNotFound("할인 마스터 데이터가 존재하지 않습니다.");
            }
            referenceCache.getDiscountMap().putAll(discountMap);

            // 로깅
            log.info("마스터 데이터 캐싱 완료: 상품={}건, 할인={}건", productMap.size(), discountMap.size());
        }catch(Exception e){
            log.error(">>[Stop before job start]: {}", e.getMessage());
            errorLogger.saveForJobLevel(jobExecution, 0L, e, "BEFORE_JOB");

            throw e;
        }
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        // 필요 시 캐시 정리
        referenceCache.clear();
    }
}
