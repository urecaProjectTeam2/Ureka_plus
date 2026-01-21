package com.touplus.billing_batch.jobs.billing.step.listener;

import java.util.Map;
import java.util.stream.Collectors;

import com.touplus.billing_batch.common.BillingFatalException;
import com.touplus.billing_batch.jobs.billing.BillingReferenceCache;
import com.touplus.billing_batch.domain.dto.BillingDiscountDto;
import com.touplus.billing_batch.domain.dto.BillingProductDto;
import com.touplus.billing_batch.domain.repository.BillingDiscountRepository;
import com.touplus.billing_batch.domain.repository.BillingProductRepository;

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

    private final BillingReferenceCache referenceCache;
    private final BillingProductRepository billingProductRepository;
    private final BillingDiscountRepository billingDiscountRepository;
    private final BillingErrorLogger errorLogger;

    @Override
    @Transactional(readOnly = true) // 마스터 데이터 대량 조회 성능 최적화
    public void beforeJob(JobExecution jobExecution) {
        try {
            log.info(">>> [BeforeJob] 마스터 데이터 캐싱 시작...");

            // 1. 지역 변수(Local Map)에 먼저 데이터를 로드합니다.
            // 이렇게 하면 로딩 중에 다른 스레드가 캐시에 접근해도 안전합니다.
            Map<Long, BillingProductDto> tempProductMap = billingProductRepository.findAll()
                    .stream()
                    .map(BillingProductDto::fromEntity)
                    .collect(Collectors.toMap(BillingProductDto::getProductId, p -> p));

            if (tempProductMap.isEmpty()) {
                throw BillingFatalException.cacheNotFound("상품 마스터 데이터가 존재하지 않습니다.");
            }

            Map<Long, BillingDiscountDto> tempDiscountMap = billingDiscountRepository.findAll()
                    .stream()
                    .map(BillingDiscountDto::fromEntity)
                    .collect(Collectors.toMap(BillingDiscountDto::getDiscountId, d -> d));

            if (tempDiscountMap.isEmpty()) {
                throw BillingFatalException.cacheNotFound("할인 마스터 데이터가 존재하지 않습니다.");
            }

            // 2. 준비된 데이터를 캐시 객체의 update 메서드를 통해 한 번에 교체합니다.
            // (Atomic-like 교체: 중간에 비어있는 맵을 참조할 일이 없습니다.)
            referenceCache.updateProducts(tempProductMap);
            referenceCache.updateDiscounts(tempDiscountMap);

            log.info(">>> [BeforeJob] 마스터 데이터 캐싱 완료: 상품={}건, 할인={}건",
                    tempProductMap.size(), tempDiscountMap.size());

        } catch (Exception e) {
            log.error(">>> [BeforeJob] 캐싱 실패로 배치를 중단합니다: {}", e.getMessage());
            // 시스템 레벨 에러 로그 저장
            errorLogger.saveForJobLevel(jobExecution, 0L, e, "BEFORE_JOB");

            // RuntimeException으로 던져서 Job 실행 자체를 차단
            throw e;
        }
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        // Job 종료 후 메모리 점유를 해제하기 위해 캐시를 비웁니다.
        log.info(">>> [AfterJob] 배치가 종료되어 캐시 데이터를 정리합니다.");
        referenceCache.clear();
    }
}