package com.touplus.billing_batch.jobs.billing.step.processor;

import com.touplus.billing_batch.common.BillingException;
import com.touplus.billing_batch.common.BillingFatalException;
import com.touplus.billing_batch.domain.dto.*;
import com.touplus.billing_batch.domain.dto.SettlementDetailsDto.DetailItem;
import com.touplus.billing_batch.domain.enums.ProductType;
import com.touplus.billing_batch.domain.enums.UseType;
import com.touplus.billing_batch.jobs.billing.cache.BillingReferenceCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class AmountCalculationProcessor
        implements ItemProcessor<BillingUserBillingInfoDto, BillingWorkDto> {

    private final BillingReferenceCache referenceCache;

    @Value("#{jobParameters['targetMonth']}")
    private String targetMonth;

    @Override
    public BillingWorkDto process(BillingUserBillingInfoDto item) throws Exception {
        BillingWorkDto workDto = BillingWorkDto.builder()
                .rawData(item)
                .build();

        Map<Long, BillingProductDto> productMap = referenceCache.getProductMap();
        Map<Long, OverusePolicyDto> overusePolicyMap = referenceCache.getOverusePolicyMap();
        Map<UsageKeyDto, ProductBaseUsageDto> productBaseUsageMap = referenceCache.getProductBaseUsageMap();
        Map<Long, RefundPolicyDto> refundPolicyMap = referenceCache.getRefundPolicyMap();

        int joinedYear = 0;
        long mobileProductId = 0;

        if (productMap == null || productMap.isEmpty())
            throw BillingFatalException.cacheNotFound("상품 정보 캐싱이 이루어지지 않았습니다.");

//        log.info("[AmountCalculationProcessor] 상품 정보 가져오기 성공");

        double productSum = 0;

        // 상품 가격 합산
        // product는 무조건 1개 이상 존재.
        List<UserSubscribeProductDto> products = item.getProducts();
        if (products == null || products.isEmpty())
            throw BillingException.dataNotFound(item.getUserId(), "상품 이용 내역이 존재하지 않습니다."); //데이터가 없는 경우. error. skip

        for (UserSubscribeProductDto usp : products) {
            BillingProductDto product = productMap.get(usp.getProductId());

            if (product == null) {
                log.error("상품 정보를 찾을 수 없습니다. ProductId: {}, UserId: {}", usp.getProductId(), item.getUserId());
                throw BillingException.dataNotFound(item.getUserId(), "상품 정보(ID: " + usp.getProductId() + ")가 캐시에 없습니다.");
            }

            // 환불 체크
            if (usp.getDeletedAt() != null) {
                RefundPolicyDto refundPolicy = refundPolicyMap.get(product.getProductId());
                if (refundPolicy != null) {
                    long usedDays = ChronoUnit.DAYS.between(
                            usp.getCreatedMonth(),
                            usp.getDeletedAt()
                    );

                    if (usedDays < refundPolicy.getRefundDuration()) {
                        continue; // 환불 기간 내 → 처리
                    }
                }

            }

            // 캐시 상품이 존재하면 상세 정보 가져오기
            String productName = product.getProductName();
            ProductType productType = product.getProductType();
            double price = product.getPrice();

            // 상품 데이터 이상
            if (productType == null) {
                throw BillingException.dataNotFound(item.getUserId(), "상품 타입이 비어있습니다.");
            }

            if (productName == null || productName.isBlank()) {
                throw BillingException.dataNotFound(item.getUserId(), "상품명이 비어있습니다.");
            }

            if (price < 0) {
                throw BillingException.invalidProductData(item.getUserId(), String.valueOf(usp.getProductId()));
            }

            // 장기 할인에 대한 사용일 계산
            if (productType == ProductType.mobile || productType == ProductType.internet) {
                LocalDate createdMonth = usp.getCreatedMonth();
                LocalDate deletedAt = usp.getDeletedAt() == null
                        ? LocalDate.parse(targetMonth).withDayOfMonth(
                        LocalDate.parse(targetMonth).lengthOfMonth()
                )
                        : usp.getDeletedAt();
                long daysUsed = ChronoUnit.DAYS.between(createdMonth, deletedAt);
                int yearsUsed = (int) (daysUsed / 365); // 365일 기준
                joinedYear += yearsUsed;
            }

            // MOBILE 구독 상품이 있으면 productId 저장
            if (productType == ProductType.mobile) mobileProductId = usp.getProductId();

            productSum += price;
            DetailItem detail = DetailItem.builder()
                    .productType(productType.name().toUpperCase())
                    .productName(productName)
                    .price((int)price)
                    .build();

            // 상세 내역 생성 (JSON 최종용)
            // 타입별로 분류
            switch (productType) {
                case mobile -> workDto.getMobile().add(detail);
                case internet -> workDto.getInternet().add(detail);
                case iptv -> workDto.getIptv().add(detail);
                case dps -> workDto.getDps().add(detail);
                case addon -> workDto.getAddon().add(detail);
            }
        }

//        log.info("[AmountCalculationProcessor] 상품 금액 합산 완료");

        // 추가 요금이 없으면 빈 리스트 처리
        List<AdditionalChargeDto> charges =
                item.getAdditionalCharges() == null ? Collections.emptyList() : item.getAdditionalCharges();

        // 추가 요금 합산
        double additionalSum = 0;
        for (AdditionalChargeDto ac : charges) {
            //추가 요금 데이터 이상.
            if (ac == null) {
                throw BillingException.dataNotFound(item.getUserId(), "추가요금 Row가 비어있습니다.");
            }

            if (ac.getPrice() < 0) {
                throw BillingException.invalidAdditionalChargeData(item.getUserId(), String.valueOf(ac.getId()));
            }

            if (ac.getCompanyName() == null || ac.getCompanyName().isBlank()) {
                throw BillingException.dataNotFound(item.getUserId(), "추가요금 결제사명이 비어있습니다.");
            }

            additionalSum += ac.getPrice();
            workDto.getAddon().add(DetailItem.builder()
                    .productType("ADDITIONAL_CHARGE")
                    .productName(ac.getCompanyName())
                    .price(ac.getPrice())
                    .build());
        }

//        log.info("[AmountCalculationProcessor] 추가 요금 합산 완료");

        // 월 기본 사용량 기반 추가 사용량 정산

        // 1. 월 기본 사용량 리스트가 비었느지 확인 -> 비었으면 종료 예외 처리
        if (overusePolicyMap == null || overusePolicyMap.isEmpty()) {
            throw BillingFatalException.cacheNotFound("초과 요금 정책 캐시가 비어 있습니다.");
        }

        if (productBaseUsageMap == null || productBaseUsageMap.isEmpty()) {
            throw BillingFatalException.cacheNotFound("상품별 기본 사용량 캐시가 비어 있습니다.");
        }

        // 2. 사용자당 월 사용량 리스트가 비었는지 확인 -> 비었으면 종료 예외 처리
        List<UserUsageDto> userUsages = item.getUsage();

        if (userUsages == null || userUsages.isEmpty()) {
            throw BillingException.dataNotFound(item.getUserId(),"사용자별 사용량 데이터가 존재하지 않습니다.");
        }
        // 3. 사용자 요금 확인 -> 무제한이면 건너뛰기(혹은 기본 사용량 테이블에 값이 없으면 건너뛰기)
        for (UserUsageDto us : userUsages) {
            if (mobileProductId == 0) break; // 모바일 상품 이용을 안하는 경우
            UsageKeyDto usageKey = new UsageKeyDto(mobileProductId, us.getUseType());

            // 해당 상품에 대한 기본 사용량 정책 가져오기
            ProductBaseUsageDto baseUsageDto = productBaseUsageMap.get(usageKey);

            if (baseUsageDto == null) continue; // 무제한이면 다음으로 넘어가기

            OverusePolicyDto policy = overusePolicyMap.get(baseUsageDto.getOverusePolicyId());
            if (policy == null) {
                log.warn("사용유형 {}에 대한 초과 요금 정책이 없습니다.", us.getUseType());
                continue;
            }

            // 초과 요금이 있으면 추가요금 계산
            double basicAmount = baseUsageDto.getBasicAmount();
            double useAmount = us.getUseAmount();
            // 실제 사용량 < 기본 사용량이면 continue
            if(basicAmount < useAmount) {
                // 실제 사용량 > 기본 사용량인 경우
                double overCharge = (useAmount - basicAmount) * policy.getUnitPrice();

                additionalSum += overCharge;
                workDto.getAddon().add(DetailItem.builder()
                        .productType("OVERUSE_CHARGE")
                        .productName(us.getUseType().name() + "초과 요금")
                        .price((int)overCharge)
                        .build());
            }
        }

        // 정산 로직 이상 탐지
        double baseAmount = productSum + additionalSum;
        if (productSum < 0 || additionalSum < 0 || baseAmount < 0) {
            throw BillingFatalException.invalidProductAmount(item.getUserId(), productSum, additionalSum, baseAmount);
        }

        // 정산이 없음.
        if (baseAmount == 0) {
            throw BillingException.NoSettlementFee(item.getUserId());
        }

        workDto.setProductAmount(productSum);
        workDto.setAdditionalCharges(additionalSum);

        // 총 상품 금액 + 총 추가요금
        workDto.setBaseAmount(baseAmount);

//        log.info("[AmountCalculationProcessor] DTO에 상품/추가요금 내역 저장");

        workDto.setJoinedYear(joinedYear);
        return workDto;
    }
}
