package com.touplus.billing_batch.jobs.billing.step.processor;

import com.touplus.billing_batch.domain.dto.BillingWorkDto;
import com.touplus.billing_batch.domain.dto.SettlementDetailsDto;
import com.touplus.billing_batch.domain.dto.UserSubscribeDiscountDto;
import com.touplus.billing_batch.domain.enums.DiscountType;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@StepScope
public class DiscountCalculationProcessor
        implements ItemProcessor<BillingWorkDto, BillingWorkDto> {

    @Override
    public BillingWorkDto process(BillingWorkDto work) throws Exception {
        // 할인&상품 관련 데이터 가져오기
        List<UserSubscribeDiscountDto> discounts = work.getRawData().getDiscounts();

        // 할인금액 합산 변수
        int totalDiscount = 0;

        for (UserSubscribeDiscountDto usd : discounts) {
            int price = 0;
            if (usd.getIsCash() == DiscountType.CASH) {
                // 정해진 금액 할인
                price = usd.getCash();
            } else if (usd.getIsCash() == DiscountType.RATE) {
                // 특정 비율 할인
                price = (int) (usd.getProductPrice() * usd.getPercent() * 0.01);
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
        work.setDiscountAmount(totalDiscount);

        int totalPrice = work.getBaseAmount() - work.getDiscountAmount();
        work.setTotalPrice(Math.max(0, totalPrice));

        return work;
    }
}
