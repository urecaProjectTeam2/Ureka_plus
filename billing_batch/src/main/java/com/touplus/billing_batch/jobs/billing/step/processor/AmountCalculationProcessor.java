package com.touplus.billing_batch.jobs.billing.step.processor;

import com.touplus.billing_batch.common.BillingException;
import com.touplus.billing_batch.common.BillingFatalException;
import com.touplus.billing_batch.domain.dto.AdditionalChargeDto;
import com.touplus.billing_batch.domain.dto.BillingUserBillingInfoDto;
import com.touplus.billing_batch.domain.dto.BillingWorkDto;
import com.touplus.billing_batch.domain.dto.SettlementDetailsDto.DetailItem;
import com.touplus.billing_batch.domain.dto.UserSubscribeProductDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
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
        // product는 무조건 1개 이상 존재.
        List<UserSubscribeProductDto> products = item.getProducts();
        if(products == null || products.isEmpty())
            throw BillingException.dataNotFound(item.getUserId(), "상품 이용 내역이 존재하지 않습니다."); //데이터가 없는 경우. error. skip

        long productSum = 0;
        for (UserSubscribeProductDto usp : item.getProducts()) {

            // 상품 데이터 이상
            if (usp == null) {
                throw BillingException.dataNotFound(item.getUserId(), "상품 Row가 비어있습니다.");
            }

            if (usp.getProductType() == null) {
                throw BillingException.dataNotFound(item.getUserId(), "상품 타입이 비어있습니다.");
            }

            if (usp.getProductName() == null || usp.getProductName().isBlank()) {
                throw BillingException.dataNotFound(item.getUserId(), "상품명이 비어있습니다.");
            }

            if (usp.getPrice() < 0) {
                throw BillingException.invalidProductData(item.getUserId(), String.valueOf(usp.getProductId()));
            }

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


        
        // 추가 요금이 없으면 빈 리스트 처리
        List<AdditionalChargeDto> charges =
                item.getAdditionalCharges() == null ? Collections.emptyList() : item.getAdditionalCharges();
        
        // 추가 요금 합산
        long additionalSum = 0;
        for (AdditionalChargeDto ac : charges) {
            //추가 요금 데이터 이상.
            if(ac==null){
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

        // 정산 로직 이상 탐지
        long baseAmount = productSum + additionalSum;
        if(productSum<0 || additionalSum<0 || baseAmount <0 || baseAmount > Integer.MAX_VALUE){
            throw BillingFatalException.invalidProductAmount(item.getUserId(), productSum, additionalSum, baseAmount);
        }

        workDto.setProductAmount((int)productSum);
        workDto.setAdditionalCharges((int)additionalSum);

        // 총 상품 금액 + 총 추가요금
        workDto.setBaseAmount((int)baseAmount);

        return workDto;
    }

}
