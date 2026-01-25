package com.touplus.billing_batch.jobs.billing.cache;

import com.touplus.billing_batch.common.BillingFatalException;
import com.touplus.billing_batch.domain.dto.*;
import com.touplus.billing_batch.domain.entity.DiscountPolicy;
import com.touplus.billing_batch.domain.enums.UseType;
import com.touplus.billing_batch.domain.repository.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingReferenceLoader {
    private final BillingReferenceCache cache;
    private final BillingProductRepository productRepository;
    private final BillingDiscountRepository discountRepository;
    private final ProductBaseUsageRepository productBaseUsageRepository;
    private final RefundPolicyRepository refundPolicyRepository;
    private final OverusePolicyRepository overusePolicyRepository;
    private final DiscountPolicyRepository discountPolicyRepository;


    @Transactional(readOnly = true)
    public void loadOrThrow() {
        log.info(">>> [Cache] 마스터 데이터 로딩 시작");

        Map<Long, BillingProductDto> productMap =
                productRepository.findAll().stream()
                        .map(BillingProductDto::fromEntity)
                        .collect(Collectors.toMap(
                                BillingProductDto::getProductId,
                                p -> p
                        ));

        if (productMap.isEmpty()) {
            throw BillingFatalException.cacheNotFound("상품 마스터 데이터가 존재하지 않습니다.");
        }

        Map<Long, BillingDiscountDto> discountMap =
                discountRepository.findAll().stream()
                        .map(BillingDiscountDto::fromEntity)
                        .collect(Collectors.toMap(
                                BillingDiscountDto::getDiscountId,
                                d -> d
                        ));

        if (discountMap.isEmpty()) {
            throw BillingFatalException.cacheNotFound("할인 마스터 데이터가 존재하지 않습니다.");
        }

        Map<UsageKeyDto, ProductBaseUsageDto> productBaseUsageMap =
                productBaseUsageRepository.findAll().stream()
                        .map(ProductBaseUsageDto::fromEntity)
                        .collect(Collectors.toMap(
                                d -> new UsageKeyDto(d.getProductId(), d.getUseType()),
                                d -> d
                        ));

        if (productBaseUsageMap.isEmpty()) {
            throw BillingFatalException.cacheNotFound("상품 기본 제공 사용량 데이터가 존재하지 않습니다.");
        }

        Map<Long, RefundPolicyDto> refundPolicyMap =
                refundPolicyRepository.findAll().stream()
                        .map(RefundPolicyDto::fromEntity)
                        .collect(Collectors.toMap(
                                RefundPolicyDto::getProductId,
                                d -> d
                        ));

        if (refundPolicyMap.isEmpty()) {
            throw BillingFatalException.cacheNotFound("환불 정책 데이터가 존재하지 않습니다.");
        }

        Map<UseType, OverusePolicyDto> overusePolicyMap =
                overusePolicyRepository.findAll().stream()
                        .map(OverusePolicyDto::fromEntity)
                        .collect(Collectors.toMap(
                                OverusePolicyDto::getUseType,
                                d -> d
                        ));

        if (overusePolicyMap.isEmpty()) {
            throw BillingFatalException.cacheNotFound("상품 초과 사용량 정책 데이터가 존재하지 않습니다.");
        }

        Map<Long, DiscountPolicyDto> discountPolicyMap =
                discountPolicyRepository.findAll().stream()
                        .map(DiscountPolicyDto::fromEntity)
                        .collect(Collectors.toMap(
                                DiscountPolicyDto::getDiscountPolicyId,
                                d -> d
                        ));

        if (discountPolicyMap.isEmpty()) {
            throw BillingFatalException.cacheNotFound("할인 정책 데이터가 존재하지 않습니다.");
        }

        cache.updateProducts(productMap);
        cache.updateDiscounts(discountMap);
        cache.updateProductBaseUsageMaps(productBaseUsageMap);
        cache.updateRefundPolicyMaps(refundPolicyMap);
        cache.updateOverusePolicyMaps(overusePolicyMap);
        cache.updateDiscountPolicyMap(discountPolicyMap);


        log.info(">>> [Cache] 로딩 완료: 상품={}건, 할인={}건, 상품 기본 제공 사용량={}건, 환불 정책 {}건, 할인 정책 {}건",
                productMap.size(), discountMap.size(), productBaseUsageMap.size(), refundPolicyMap.size(), discountPolicyMap.size());
    }

    public void clear() {
        cache.clear();
    }
}