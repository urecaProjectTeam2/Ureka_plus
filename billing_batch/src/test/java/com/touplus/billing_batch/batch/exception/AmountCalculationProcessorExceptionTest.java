package com.touplus.billing_batch.batch.exception;

import com.touplus.billing_batch.common.BillingException;
import com.touplus.billing_batch.common.BillingFatalException;
import com.touplus.billing_batch.domain.dto.*;
import com.touplus.billing_batch.domain.enums.ProductType;
import com.touplus.billing_batch.jobs.billing.cache.BillingReferenceCache;
import com.touplus.billing_batch.jobs.billing.step.processor.AmountCalculationProcessor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AmountCalculationProcessorExceptionTest {

    @InjectMocks
    private AmountCalculationProcessor processor;

    @Mock
    private BillingReferenceCache referenceCache;

    @Test
    @DisplayName("1. 상품 캐시가 비어있으면 BillingFatalException 발생")
    void process_CacheEmpty_ThrowsFatalException() {
        // Given: 캐시가 null을 반환하도록 설정
        given(referenceCache.getProductMap()).willReturn(null);
        BillingUserBillingInfoDto item = createValidItem(1659L);

        // When & Then
        assertThatThrownBy(() -> processor.process(item))
                .isExactlyInstanceOf(BillingFatalException.class)
                .hasMessageContaining("상품 정보 캐싱이 이루어지지 않았습니다");
    }

    @Test
    @DisplayName("2. 유저의 구독 상품 내역이 없으면 BillingException(Skip 대상) 발생")
    void process_NoProducts_ThrowsBillingException() {
        // Given
        setupMockCache();
        BillingUserBillingInfoDto item = BillingUserBillingInfoDto.builder()
                .userId(1659L)
                .products(new ArrayList<>()) // 빈 리스트
                .build();

        // When & Then
        assertThatThrownBy(() -> processor.process(item))
                .isExactlyInstanceOf(BillingException.class)
                .hasFieldOrPropertyWithValue("errorCode", "ERR_DATA_NOT_FOUND")
                .hasMessageContaining("상품 이용 내역이 존재하지 않습니다");
    }

    @Test
    @DisplayName("3. 추가요금 결제사명이 누락되면 BillingException 발생")
    void process_MissingCompanyName_ThrowsBillingException() {
        // Given
        setupMockCache();
        List<AdditionalChargeDto> charges = List.of(
                AdditionalChargeDto.builder().price(1000).companyName("").build() // 빈 이름
        );
        BillingUserBillingInfoDto item = createValidItem(1659L);
        ReflectionTestUtils.setField(item, "additionalCharges", charges);

        // When & Then
        assertThatThrownBy(() -> processor.process(item))
                .isExactlyInstanceOf(BillingException.class)
                .hasMessageContaining("추가요금 결제사명이 비어있습니다");
    }

    @Test
    @DisplayName("4. 총 합산 금액이 범위를 초과하면 BillingFatalException 발생")
    void process_AmountOverflow_ThrowsFatalException() {
        // Given: 상품 가격을 매우 크게 설정
        Map<Long, BillingProductDto> productMap = new HashMap<>();
        productMap.put(1L, BillingProductDto.builder()
                .productId(1L).price(Integer.MAX_VALUE).productName("비싼상품").productType(ProductType.mobile).build());
        given(referenceCache.getProductMap()).willReturn(productMap);

        // 추가 요금까지 더해서 Integer 범위를 넘기도록 설정
        List<AdditionalChargeDto> charges = List.of(AdditionalChargeDto.builder().price(1000).companyName("A사").build());
        BillingUserBillingInfoDto item = createValidItem(1659L);
        ReflectionTestUtils.setField(item, "additionalCharges", charges);

        // When & Then
        assertThatThrownBy(() -> processor.process(item))
                .isExactlyInstanceOf(BillingFatalException.class)
                .hasFieldOrPropertyWithValue("errorCode", "FATAL_INVALID_AMOUNT");
    }

    // --- Helper Methods ---
    private void setupMockCache() {
        Map<Long, BillingProductDto> productMap = new HashMap<>();
        productMap.put(1L, BillingProductDto.builder()
                .productId(1L).price(10000).productName("테스트상품").productType(ProductType.mobile).build());
        given(referenceCache.getProductMap()).willReturn(productMap);
    }

    private BillingUserBillingInfoDto createValidItem(Long userId) {
        return BillingUserBillingInfoDto.builder()
                .userId(userId)
                .products(List.of(UserSubscribeProductDto.builder().productId(1L).build()))
                .additionalCharges(new ArrayList<>())
                .build();
    }
}