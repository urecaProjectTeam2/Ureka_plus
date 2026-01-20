package com.touplus.billing_batch.jobs.billing.step.processor;

import com.touplus.billing_batch.common.BillingReferenceCache;
import com.touplus.billing_batch.domain.dto.*;
import com.touplus.billing_batch.domain.dto.SettlementDetailsDto.DetailItem;
import com.touplus.billing_batch.domain.enums.ProductType;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@StepScope
@RequiredArgsConstructor
public class AmountCalculationProcessor
        implements ItemProcessor<BillingUserBillingInfoDto, BillingWorkDto> {

    private final BillingReferenceCache referenceCache;

    @Override
    public BillingWorkDto process(BillingUserBillingInfoDto item) throws Exception {
        BillingWorkDto workDto = BillingWorkDto.builder()
                .rawData(item)
                .build();

        Map<Long, BillingProductDto> productMap = referenceCache.getProductMap();

        int productSum = 0;

        for (UserSubscribeProductDto usp : item.getProducts()) {
            BillingProductDto product = productMap.get(usp.getProductId());

            // 캐시 상품이 존재하면 상세 정보 가져오기
            String productName = product != null ? product.getProductName() : "UNKNOWN";
            ProductType productType = product != null ? product.getProductType() : null;
            int price = product != null && product.getPrice() != null ? product.getPrice() : 0;

            productSum += price;

            // 상세 내역 생성 (JSON 최종용)
            DetailItem detail = DetailItem.builder()
                    .productType(productType != null ? productType.name().toUpperCase() : "UNKNOWN")
                    .productName(productName)
                    .price(price)
                    .build();

            // 타입별로 분류
            if (productType != null) {
                switch (productType) {
                    case mobile -> workDto.getMobile().add(detail);
                    case internet -> workDto.getInternet().add(detail);
                    case iptv -> workDto.getIptv().add(detail);
                    case dps -> workDto.getDps().add(detail);
                    case addon -> workDto.getAddon().add(detail);
                }
            } else {
                workDto.getAddon().add(detail);
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
        workDto.setBaseAmount(productSum + additionalSum);
        workDto.setTotalPrice(workDto.getBaseAmount());

        return workDto;
    }
}
