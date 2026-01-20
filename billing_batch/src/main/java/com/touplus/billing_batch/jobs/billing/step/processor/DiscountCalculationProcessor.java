package com.touplus.billing_batch.jobs.billing.step.processor;

import com.touplus.billing_batch.common.BillingException;
import com.touplus.billing_batch.common.BillingFatalException;
import com.touplus.billing_batch.domain.dto.BillingWorkDto;
import com.touplus.billing_batch.domain.dto.SettlementDetailsDto;
import com.touplus.billing_batch.domain.dto.UserSubscribeDiscountDto;
import com.touplus.billing_batch.domain.enums.DiscountType;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@StepScope
public class DiscountCalculationProcessor
        implements ItemProcessor<BillingWorkDto, BillingWorkDto> {

    @Override
    public BillingWorkDto process(BillingWorkDto work) throws Exception {
        // 할인&상품 관련 데이터 가져오기
        List<UserSubscribeDiscountDto> discounts =
                work.getRawData().getDiscounts() == null ? Collections.emptyList() : work.getRawData().getDiscounts();

        // 할인금액 합산 변수
        long totalDiscount = 0;

        for (UserSubscribeDiscountDto usd : discounts) {

            // 할인 데이터 이상
            if(usd==null){
                throw BillingException.dataNotFound(work.getRawData().getUserId(), "할인 Row가 비어있습니다.");
            }

            if (usd.getIsCash() == null) {
                throw BillingException.dataNotFound(work.getRawData().getUserId(), "할인 타입이 비어있습니다.");
            }

            if (usd.getDiscountName() == null || usd.getDiscountName().isBlank()) {
                throw BillingException.dataNotFound(work.getRawData().getUserId(), "할인명이 비어있습니다.");
            }

            int price = 0;
            if (usd.getIsCash() == DiscountType.CASH) {
                // 정해진 금액 할인
                if (usd.getCash() <= 0) {
                    throw BillingException.invalidDiscountData(work.getRawData().getUserId(), String.valueOf(usd.getDiscountId()));
                }

                price = usd.getCash();
            } else if (usd.getIsCash() == DiscountType.RATE) {
                // 특정 비율 할인
                if (usd.getPercent() <= 0 || usd.getPercent() > 100) {
                    throw BillingException.invalidDiscountData(work.getRawData().getUserId(), String.valueOf(usd.getDiscountId()));
                }
                if (usd.getProductPrice() < 0) {
                    throw BillingException.invalidDiscountData(work.getRawData().getUserId(), String.valueOf(usd.getDiscountId()) + "연결된 상품에 비정상적인 데이터가 있습니다.");
                }

                price = (int) ((usd.getProductPrice() * usd.getPercent()) / 100);
            }

            totalDiscount += price;

            // 할인 상세 내역 저장
            work.getDiscounts().add(SettlementDetailsDto.DetailItem.builder()
                    .productType("DISCOUNT")
                    .productName(usd.getDiscountName())
                    .price(price * -1)
                    .build());
        }

        // 총 할인 금액 저장
        if (totalDiscount > Integer.MAX_VALUE) {
            throw BillingFatalException.invalidDiscountAmount(work.getRawData().getUserId(), totalDiscount);
        }

        work.setDiscountAmount((int)totalDiscount);

        int totalPrice = work.getBaseAmount() - work.getDiscountAmount();
        work.setTotalPrice(Math.max(0, totalPrice));

        return work;
    }
}
