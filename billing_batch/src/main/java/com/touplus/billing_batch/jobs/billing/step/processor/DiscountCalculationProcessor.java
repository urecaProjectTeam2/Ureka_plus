package com.touplus.billing_batch.jobs.billing.step.processor;

import com.touplus.billing_batch.common.BillingException;
import com.touplus.billing_batch.common.BillingFatalException;
import com.touplus.billing_batch.domain.dto.BillingWorkDto;
import com.touplus.billing_batch.domain.dto.SettlementDetailsDto;
import com.touplus.billing_batch.domain.dto.UserSubscribeDiscountDto;
import com.touplus.billing_batch.domain.enums.CalOrderType;
import com.touplus.billing_batch.domain.enums.DiscountRangeType;
import com.touplus.billing_batch.jobs.billing.cache.BillingReferenceCache;
import com.touplus.billing_batch.domain.dto.*;
import com.touplus.billing_batch.domain.enums.DiscountType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class DiscountCalculationProcessor implements ItemProcessor<BillingWorkDto, BillingWorkDto> {

    private final BillingReferenceCache referenceCache;

    @Override
    public BillingWorkDto process(BillingWorkDto work) throws Exception {
        // 할인&상품 관련 데이터 가져오기
        Queue<UserSubscribeDiscountDto> discounts =
                work.getRawData().getDiscounts() == null
                        ? new LinkedList<>()
                        : new LinkedList<>(work.getRawData().getDiscounts());


//        List<UserSubscribeDiscountDto> discounts =
//                work.getRawData().getDiscounts() == null ? Collections.emptyList() : work.getRawData().getDiscounts();

        // 캐시 미리 가져오기
        Map<Long, BillingDiscountDto> discountMap = referenceCache.getDiscountMap();
        Map<Long, BillingProductDto> productMap = referenceCache.getProductMap();
        Map<Long, DiscountPolicyDto> discountPolicyMap = referenceCache.getDiscountPolicyMap();

        if (discountMap == null || discountMap.isEmpty())
            throw BillingFatalException.cacheNotFound("할인 정보 캐싱이 이루어지지 않았습니다.");
        if (productMap == null || productMap.isEmpty())
            throw BillingFatalException.cacheNotFound("상품 정보 캐싱이 이루어지지 않았습니다.");
        if (discountMap == null || discountMap.isEmpty()) {
            throw BillingFatalException.cacheNotFound("할인 정책 캐싱이 이루어지지 않았습니다.");
        }

        Map<Long, Double> productPriceMap =
                discounts.stream()
                        .map(UserSubscribeDiscountDto::getProductId)
                        .distinct() // 같은 productId 중복 방지 (선택)
                        .collect(Collectors.toMap(
                                productId -> productId,
                                productId -> {
                                    BillingProductDto product = productMap.get(productId);
                                    return Double.valueOf(product.getPrice());
                                }
                        ));

        // 할인금액 합산 변수
        double totalDiscount = 0;
        int joinedYear = work.getJoinedYear();
        long userId = work.getRawData().getUserId();

        BillingUserMemberDto userGroup = work.getRawData().getUsers();

        // 결합 할인을 위해 모바일/인터냇 보유 여부 확인용
        boolean hasMobile = false;
        boolean hasInternet = false;
        long mobilePrice = 0;
        long internetPrice = 0;
        int skipCnt = 0;

        Queue<UserSubscribeDiscountDto> multi = new LinkedList<>();
        Queue<UserSubscribeDiscountDto> singleRate = new LinkedList<>();

        while (!discounts.isEmpty()) {
            UserSubscribeDiscountDto usd = discounts.poll();

            // 캐시에서 상품 정보 가져오기
            BillingProductDto product = productMap.get(usd.getProductId());
            BillingDiscountDto discount = discountMap.get(usd.getDiscountId());
            DiscountPolicyDto discountPolicy = discountPolicyMap.get(usd.getDiscountId());

            if (product == null) {
                throw BillingException.dataNotFound(userId, "상품 정보(ID: " + usd.getProductId() + ")가 캐시에 없습니다.");
            }
            if (discount == null) {
                throw BillingException.dataNotFound(userId, "할인 정보(ID: " + usd.getDiscountId() + ")가 캐시에 없습니다.");
            }
            if (discountPolicy == null) {
                throw BillingException.dataNotFound(userId, "할인 정책 정보(ID: " + usd.getDiscountId() + ")가 캐시에 없습니다.");
            }

            // 할인 데이터 이상
            if (discount.getIsCash() == null) {
                throw BillingException.dataNotFound(userId, "할인 타입이 비어있습니다.");
            }

            if (discount.getDiscountName() == null || discount.getDiscountName().isBlank()) {
                throw BillingException.dataNotFound(userId, "할인명이 비어있습니다.");
            }

            // addon 은 할인에서 제외
            String productType = product.getProductType().toString().toLowerCase();
            if ("addon".equals(productType)) {
                continue;
            }

            // 할인 조건 분기처리
            // 1. Single Cash
            if (discountPolicy.getCalOrder() == CalOrderType.SINGLE && discount.getIsCash() == DiscountType.CASH) {
                if (discount.getCash() == null || discount.getCash() <= 0) {
                    throw BillingException.invalidDiscountData(userId, String.valueOf(usd.getDiscountId()));
                }
                Long productId = usd.getProductId();
                // map 안의 price에서 할인 금액 빼기
                // 할인 금액
                double discountAmount = discountMap.get(usd.getDiscountId()).getCash();
                double nowProductPrice = productPriceMap.get(productId);
                productPriceMap.replace(productId, nowProductPrice - discountAmount);

                // totalDiscount에 할인 금액 더하기
                totalDiscount += discountAmount;

                // 할인 상세내역 기록
                saveDiscountDetails(work, discountPolicy.getDiscountRange(), discount.getDiscountName(), discountAmount);
            }
            // 2. Single Ratio
            else if (discountPolicy.getCalOrder() == CalOrderType.SINGLE && discount.getIsCash() == DiscountType.RATE) {
                singleRate.offer(usd);
            }
            // 3. Multi
            else if (discountPolicy.getCalOrder() == CalOrderType.MULTI) {
                multi.offer(usd);
            } else {
                throw BillingException.invalidDiscountCondition(userId, discount.getDiscountId());
            }
        }

        // single Queue poll
        while (!singleRate.isEmpty()) {
            UserSubscribeDiscountDto usd = singleRate.poll();

            // 캐시에서 상품 정보 가져오기
            BillingProductDto product = productMap.get(usd.getProductId());
            BillingDiscountDto discount = discountMap.get(usd.getDiscountId());
            DiscountPolicyDto discountPolicy = discountPolicyMap.get(usd.getDiscountId());

            String contentType = discount.getContentType().toString().toLowerCase();
            int configValue = discount.getValue() == null ? 0 : discount.getValue(); // 할인 value

            if ("group".equals(contentType)) {
                if (userGroup == null || userGroup.getGroupId() != null ||
                        userGroup.getGroupNumOfMember() != configValue ||
                        userGroup.getUserNumOfMember() != configValue){
                    throw BillingException.invalidDiscountCondition(userId, discount.getDiscountId());
                }
            }

            double discountPercent = discount.getPercent();
            if (discountPercent <= 0 || discountPercent > 100) {
                throw BillingException.invalidDiscountData(userId, String.valueOf(usd.getDiscountId()));
            }

            double originalProductPrice = product.getPrice();  // 원가
            double calculateDiscount = (originalProductPrice * discountPercent) / 100.0;    // 할인액

            double nowProductPrice = productPriceMap.get(usd.getProductId());
            productPriceMap.replace(usd.getProductId(), nowProductPrice - calculateDiscount);

            totalDiscount += calculateDiscount;

            saveDiscountDetails(work, discountPolicy.getDiscountRange(), discount.getDiscountName(), calculateDiscount);
        }

        // multi Queue poll
        while (!multi.isEmpty()) {
            UserSubscribeDiscountDto usd = multi.poll();

            // 캐시에서 상품 정보 가져오기
            BillingProductDto product = productMap.get(usd.getProductId());
            BillingDiscountDto discount = discountMap.get(usd.getDiscountId());
            DiscountPolicyDto discountPolicy = discountPolicyMap.get(usd.getDiscountId());

            String contentType = discount.getContentType().toString().toLowerCase(); // year group
            int configValue = discount.getValue() == null ? 0 : discount.getValue(); // 할인 value

            if (discountPolicy.getDiscountRange() == DiscountRangeType.MOBILE_INTERNET) {
                if ("year".equals(contentType)) {
                    if (joinedYear < configValue) { // 장기 할인 혜택 오류
                        throw BillingException.invalidDiscountCondition(userId, discount.getDiscountId());
                    }
                }
                if (discount.getIsCash() == DiscountType.RATE) {
                    double discountPercent = (discountMap.get(usd.getDiscountId()).getPercent()) * 0.01;
                    double nowPrice = productPriceMap.get(usd.getProductId()); // map 기준
                    double price = nowPrice * discountPercent;  // 할인액

                    productPriceMap.replace(usd.getProductId(), nowPrice - price);
                    totalDiscount += price;
                    saveDiscountDetails(work, discountPolicy.getDiscountRange(), discount.getDiscountName(), price);
                }
            }
        }

        // 총 할인 금액 저장
        if (totalDiscount > Integer.MAX_VALUE) {
            throw BillingFatalException.invalidDiscountAmount(userId, totalDiscount);
        }

        work.setDiscountAmount(totalDiscount);
        // 총 정산 금액 업데이트
        work.setTotalPrice(Math.max(0, work.getBaseAmount() - work.getDiscountAmount()));

        return work;
    }

    private void saveDiscountDetails(BillingWorkDto work, DiscountRangeType discountRangeType, String discountName, double price){
        // 할인 상세 내역 저장
        work.getDiscounts().add(SettlementDetailsDto.DetailItem.builder()
                .productType("DISCOUNT_" + discountRangeType)
                .productName(discountName)
                .price(price * -1)
                .build());
    }
}