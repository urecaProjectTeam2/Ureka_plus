package com.touplus.billing_batch.jobs.billing.step.processor;

import com.touplus.billing_batch.domain.dto.BillingCalculationResult;
import com.touplus.billing_batch.domain.dto.BillingUserBillingInfoDto;
import com.touplus.billing_batch.domain.dto.BillingWorkDto;
import com.touplus.billing_batch.domain.entity.BillingDiscount;
import com.touplus.billing_batch.domain.entity.UserSubscribeDiscount;
import com.touplus.billing_batch.domain.entity.UserSubscribeProduct;
import com.touplus.billing_batch.domain.enums.DiscountType;
import org.springframework.batch.item.ItemProcessor;

import java.util.List;

public class DiscountCalculationProcessor
        implements ItemProcessor<BillingWorkDto, BillingWorkDto> {

    @Override
    public BillingWorkDto process(BillingWorkDto item) throws Exception {
        // 할인&상품 관련 데이터 가져오기
        List<UserSubscribeDiscount> discounts = item.getRawData().getDiscounts();
        List<UserSubscribeProduct> products = item.getRawData().getProducts();

        // 할인금액 합산 변수
        int totalDiscount = 0;
        
        for (UserSubscribeDiscount usd : discounts) {
            BillingDiscount billingDiscount = usd.getBillingDiscount();

            if(billingDiscount.getIsCash() == DiscountType.CASH){
                totalDiscount += billingDiscount.getCash();
            }else if(billingDiscount.getIsCash() == DiscountType.RATE){
                // 연결된 상품 가져오기
                Long targetProductId = usd.getProductId();

                // 구독 중인 상품 목록에서 상품 찾기
                int productPrice = products.stream()
                        .filter(p -> p.getProductId())


                totalDiscount += (int)(products.g)
            }
        }
        // 총 할인 금액 계산
        int totalDiscount;

        if (item.getRawData().)

        return null;
    }
}
