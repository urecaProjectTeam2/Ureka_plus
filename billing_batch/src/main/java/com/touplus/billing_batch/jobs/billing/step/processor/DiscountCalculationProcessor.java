package com.touplus.billing_batch.jobs.billing.step.processor;

import com.touplus.billing_batch.common.BillingException;
import com.touplus.billing_batch.common.BillingFatalException;
import com.touplus.billing_batch.domain.dto.BillingWorkDto;
import com.touplus.billing_batch.domain.dto.SettlementDetailsDto;
import com.touplus.billing_batch.domain.dto.UserSubscribeDiscountDto;
import com.touplus.billing_batch.common.BillingReferenceCache;
import com.touplus.billing_batch.domain.dto.*;
import com.touplus.billing_batch.domain.enums.DiscountType;
import com.touplus.billing_batch.domain.enums.ProductType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class DiscountCalculationProcessor implements ItemProcessor<BillingWorkDto, BillingWorkDto> {

    private final BillingReferenceCache referenceCache;

    @Override
    public BillingWorkDto process(BillingWorkDto work) throws Exception {
        // 할인&상품 관련 데이터 가져오기
        List<UserSubscribeDiscountDto> discounts =
                work.getRawData().getDiscounts() == null ? Collections.emptyList() : work.getRawData().getDiscounts();

        // 캐시 미리 가져오기
        Map<Long, BillingDiscountDto> discountMap = referenceCache.getDiscountMap();
        Map<Long, BillingProductDto>  productMap = referenceCache.getProductMap();

        if(discountMap == null || discountMap.isEmpty())
            throw BillingFatalException.cacheNotFound("할인 정보 캐싱이 이루어지지 않았습니다.");
        if(productMap == null || productMap.isEmpty())
            throw BillingFatalException.cacheNotFound("상품 정보 캐싱이 이루어지지 않았습니다.");
        
        // 할인금액 합산 변수
        long totalDiscount = 0;

        for (UserSubscribeDiscountDto usd : discounts) {

            // 캐시에서 상품 정보 가져오기
            BillingProductDto product = productMap.get(usd.getProductId());
            BillingDiscountDto discount = discountMap.get(usd.getDiscountId());

            if (product == null) {
                throw BillingException.dataNotFound(work.getRawData().getUserId(), "상품 정보(ID: " + usd.getProductId() + ")가 캐시에 없습니다.");
            }
            if (discount == null) {
                throw BillingException.dataNotFound(work.getRawData().getUserId(), "할인 정보(ID: " + usd.getDiscountId() + ")가 캐시에 없습니다.");
            }

            // 할인 데이터 이상
            if (discount.getIsCash() == null) {
                throw BillingException.dataNotFound(work.getRawData().getUserId(), "할인 타입이 비어있습니다.");
            }

            if (discount.getDiscountName() == null || discount.getDiscountName().isBlank()) {
                throw BillingException.dataNotFound(work.getRawData().getUserId(), "할인명이 비어있습니다.");
            }

            // 할인 금액 계산
            int price = 0;
            if (discount.getIsCash() == DiscountType.CASH && discount.getCash() != null) {
                // 정해진 금액 할인
                if (discount.getCash() <= 0) {
                    throw BillingException.invalidDiscountData(work.getRawData().getUserId(), String.valueOf(usd.getDiscountId()));
                }

                price = discount.getCash();
            } else if (discount.getIsCash() == DiscountType.RATE && discount.getPercent() != null) {
                // 특정 비율 할인
                if (discount.getPercent() <= 0 || discount.getPercent() > 100) {
                    throw BillingException.invalidDiscountData(work.getRawData().getUserId(), String.valueOf(usd.getDiscountId()));
                }
                if (product.getPrice() < 0) {
                    throw BillingException.invalidProductData(work.getRawData().getUserId(), String.valueOf(usd.getDiscountId()));
                }
                price = (int) ((product.getPrice() * discount.getPercent()) / 100);
            } else {
                throw BillingException.invalidDiscountData(work.getRawData().getUserId(), String.valueOf(usd.getDiscountId()));
            }


            totalDiscount += price;

            // 할인 상세 내역 저장
            work.getDiscounts().add(SettlementDetailsDto.DetailItem.builder()
                    .productType("DISCOUNT")
                    .productName(discount.getDiscountName())
                    .price(price * -1)
                    .build());
        }

        // 총 할인 금액 저장
        if (totalDiscount > Integer.MAX_VALUE) {
            throw BillingFatalException.invalidDiscountAmount(work.getRawData().getUserId(), totalDiscount);
        }

        work.setDiscountAmount((int)totalDiscount);

        // 총 정산 금액 업데이트
        int totalPrice = work.getBaseAmount() - work.getDiscountAmount();
        work.setTotalPrice(Math.max(0, totalPrice));

        return work;
    }
}
