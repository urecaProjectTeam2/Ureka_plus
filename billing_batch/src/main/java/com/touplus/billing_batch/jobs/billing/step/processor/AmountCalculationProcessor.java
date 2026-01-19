package com.touplus.billing_batch.jobs.billing.step.processor;

import com.touplus.billing_batch.domain.dto.BillingCalculationResult;
import com.touplus.billing_batch.domain.dto.BillingUserBillingInfoDto;
import com.touplus.billing_batch.domain.dto.BillingWorkDto;
import com.touplus.billing_batch.domain.entity.AdditionalCharge;
import com.touplus.billing_batch.domain.entity.UserSubscribeProduct;
import org.springframework.batch.item.ItemProcessor;

public class AmountCalculationProcessor
        implements ItemProcessor<BillingUserBillingInfoDto, BillingWorkDto> {

    @Override
    public BillingWorkDto process(BillingUserBillingInfoDto item) throws Exception {
        // 상품 가격 합산
        int productSum = item.getProducts().stream()
                .mapToInt(p -> p.getProduct().getPrice())
                .sum();

        // 추가 요금 합산
        int additionalChargeSum = item.getAdditionalCharges().stream()
                .mapToInt(AdditionalCharge::getPrice)
                .sum();

        // 총 상품 금액 + 총 추가요금

        return BillingWorkDto.builder()
                .rawData(item)
                .productAmount(productSum)
                .additionalCharges(additionalChargeSum)
                .build();
    }

}
