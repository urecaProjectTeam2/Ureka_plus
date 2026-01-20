package com.touplus.billing_batch.jobs.billing.step.processor;

import com.touplus.billing_batch.domain.dto.AdditionalChargeDto;
import com.touplus.billing_batch.domain.dto.BillingUserBillingInfoDto;
import com.touplus.billing_batch.domain.dto.BillingWorkDto;
import com.touplus.billing_batch.domain.dto.SettlementDetailsDto.DetailItem;
import com.touplus.billing_batch.domain.dto.UserSubscribeProductDto;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
@StepScope
public class AmountCalculationProcessor
        implements ItemProcessor<BillingUserBillingInfoDto, BillingWorkDto> {

    @Override
    public BillingWorkDto process(BillingUserBillingInfoDto item) throws Exception {
        BillingWorkDto workDto = BillingWorkDto.builder()
                .rawData(item)
                .build();

        // 상품 가격 합산
        int productSum = 0;
        for (UserSubscribeProductDto usp : item.getProducts()) {
            productSum += usp.getPrice();
            DetailItem detail = DetailItem.builder()
                    .productType(usp.getProductType().name().toUpperCase())
                    .productName(usp.getProductName())
                    .price(usp.getPrice())
                    .build();

            // 상품 타입별로 분류
            switch (usp.getProductType()) {
                case mobile -> workDto.getMobile().add(detail);
                case internet -> workDto.getInternet().add(detail);
                case iptv -> workDto.getIptv().add(detail);
                case dps -> workDto.getDps().add(detail);
                case addon -> workDto.getAddon().add(detail);
            }
        }

        workDto.setProductAmount(productSum);

        // 추가 요금 합산
        int additionalSum = 0;
        for (AdditionalChargeDto ac : item.getAdditionalCharges()) {
            additionalSum += ac.getPrice();
            workDto.getAddon().add(DetailItem.builder()
                    .productType("ADDITIONAL_CHARGE")
                    .productName(ac.getCompanyName())
                    .price(ac.getPrice())
                    .build());
        }

        workDto.setAdditionalCharges(additionalSum);

        // 총 상품 금액 + 총 추가요금
        workDto.setBaseAmount(productSum + additionalSum);

        return workDto;
    }

}
