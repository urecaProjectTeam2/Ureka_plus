package com.touplus.billing_batch.jobs.billing.step.processor;

import com.touplus.billing_batch.common.BillingReferenceCache;
import com.touplus.billing_batch.domain.dto.*;
import com.touplus.billing_batch.domain.enums.DiscountType;
import com.touplus.billing_batch.domain.enums.ProductType;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@StepScope
@RequiredArgsConstructor
public class DiscountCalculationProcessor implements ItemProcessor<BillingWorkDto, BillingWorkDto> {

    private final BillingReferenceCache referenceCache;

    @Override
    public BillingWorkDto process(BillingWorkDto work) throws Exception {

        List<UserSubscribeDiscountDto> discounts = work.getRawData().getDiscounts();

        // 캐시 미리 가져오기
        Map<Long, BillingDiscountDto> discountMap = referenceCache.getDiscountMap();
        Map<Long, BillingProductDto>  productMap = referenceCache.getProductMap();

        int totalDiscount = 0;

        for (UserSubscribeDiscountDto usd : discounts) {

            // 캐시에서 상품 정보 가져오기
            BillingProductDto product = productMap.get(usd.getProductId());
            BillingDiscountDto discount = discountMap.get(usd.getDiscountId());

            String productName = product != null ? product.getProductName() : "UNKNOWN";
            ProductType productType = product != null ? product.getProductType() : null;
            int productPrice = product != null && product.getPrice() != null ? product.getPrice() : 0;

            // 할인 금액 계산
            int price = 0;
            if (discount != null) {
                if (discount.getIsCash() == DiscountType.CASH && discount.getCash() != null) {
                    price = discount.getCash();
                } else if (discount.getIsCash() == DiscountType.RATE && discount.getPercent() != null) {
                    price = (int) (productPrice * discount.getPercent() * 0.01);
                }
            }

            totalDiscount += price;

            // DetailItem에 넣기
            work.getDiscounts().add(SettlementDetailsDto.DetailItem.builder()
                    .productType("DISCOUNT")
                    .productName(discount.getDiscountName())
                    .price(price * -1)
                    .build());
        }

        work.setDiscountAmount(totalDiscount);

        // 총 정산 금액 업데이트
        int totalPrice = work.getBaseAmount() - work.getDiscountAmount();
        work.setTotalPrice(Math.max(0, totalPrice));

        return work;
    }
}
