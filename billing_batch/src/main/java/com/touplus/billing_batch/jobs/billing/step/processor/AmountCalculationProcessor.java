package com.touplus.billing_batch.jobs.billing.step.processor;

import com.touplus.billing_batch.common.BillingException;
import com.touplus.billing_batch.common.BillingReferenceCache;
import com.touplus.billing_batch.domain.dto.*;
import com.touplus.billing_batch.common.BillingException;
import com.touplus.billing_batch.common.BillingFatalException;
import com.touplus.billing_batch.domain.dto.AdditionalChargeDto;
import com.touplus.billing_batch.domain.dto.BillingUserBillingInfoDto;
import com.touplus.billing_batch.domain.dto.BillingWorkDto;
import com.touplus.billing_batch.domain.dto.SettlementDetailsDto.DetailItem;
import com.touplus.billing_batch.domain.enums.ProductType;
import lombok.RequiredArgsConstructor;
import com.touplus.billing_batch.domain.dto.UserSubscribeProductDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.Map;

import java.util.Collections;
import java.util.List;

@Slf4j
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
        if(productMap == null || productMap.isEmpty())
            throw BillingFatalException.cacheNotFound("상품 정보 캐싱이 이루어지지 않았습니다.");

        long productSum = 0;

        // 상품 가격 합산
        // product는 무조건 1개 이상 존재.
        List<UserSubscribeProductDto> products = item.getProducts();
        if(products == null || products.isEmpty())
            throw BillingException.dataNotFound(item.getUserId(), "상품 이용 내역이 존재하지 않습니다."); //데이터가 없는 경우. error. skip

        for (UserSubscribeProductDto usp : products) {
            BillingProductDto product = productMap.get(usp.getProductId());

            if (product == null) {
                log.error("상품 정보를 찾을 수 없습니다. ProductId: {}, UserId: {}", usp.getProductId(), item.getUserId());
                throw BillingException.dataNotFound(item.getUserId(), "상품 정보(ID: " + usp.getProductId() + ")가 캐시에 없습니다.");
            }

            // 캐시 상품이 존재하면 상세 정보 가져오기
            String productName = product.getProductName();
            ProductType productType = product.getProductType();
            int price = product.getPrice();

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

            productSum += price;
            DetailItem detail = DetailItem.builder()
                    .productType(productType.name().toUpperCase())
                    .productName(productName)
                    .price(price)
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
