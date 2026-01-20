package com.touplus.billing_batch.common.listener;

import java.util.Map;
import java.util.stream.Collectors;

import com.touplus.billing_batch.common.BillingReferenceCache;
import com.touplus.billing_batch.domain.dto.BillingDiscountDto;
import com.touplus.billing_batch.domain.dto.BillingProductDto;
import com.touplus.billing_batch.domain.repository.BillingDiscountRepository;
import com.touplus.billing_batch.domain.repository.BillingProductRepository;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BillingJobListener implements JobExecutionListener {

    private final BillingReferenceCache referenceCache;
    private final BillingProductRepository billingProductRepository;
    private final BillingDiscountRepository billingDiscountRepository;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        // 캐시 초기화
        referenceCache.clear();

        // 상품 캐싱
        Map<Long, BillingProductDto> productMap = billingProductRepository.findAll()
                .stream()
                .map(BillingProductDto::fromEntity)
                .collect(Collectors.toMap(BillingProductDto::getProductId, p -> p));
        referenceCache.getProductMap().putAll(productMap);

        // 할인 캐싱
        Map<Long, BillingDiscountDto> discountMap = billingDiscountRepository.findAll()
                .stream()
                .map(BillingDiscountDto::fromEntity)
                .collect(Collectors.toMap(BillingDiscountDto::getDiscountId, d -> d));
        referenceCache.getDiscountMap().putAll(discountMap);

        // 로깅
        System.out.println("마스터 데이터 캐싱 완료: 상품=" + productMap.size() + "건, 할인=" + discountMap.size() + "건");
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        // 필요 시 캐시 정리
        referenceCache.clear();
    }
}
