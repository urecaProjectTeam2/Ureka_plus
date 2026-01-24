package com.touplus.billing_batch.batch.exception;

import com.touplus.billing_batch.common.BillingException;
import com.touplus.billing_batch.common.BillingFatalException;
import com.touplus.billing_batch.domain.dto.*;
import com.touplus.billing_batch.domain.enums.DiscountType;
import com.touplus.billing_batch.domain.enums.ProductType;
import com.touplus.billing_batch.jobs.billing.cache.BillingReferenceCache;
import com.touplus.billing_batch.jobs.billing.step.processor.DiscountCalculationProcessor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class DiscountCalculationProcessorExceptionTest {

    @InjectMocks
    private DiscountCalculationProcessor processor;

    @Mock
    private BillingReferenceCache referenceCache;

    @Test
    @DisplayName("1. 할인 캐시 정보가 없으면 BillingFatalException 발생")
    void process_DiscountCacheEmpty_ThrowsFatalException() {
        // Given
        given(referenceCache.getDiscountMap()).willReturn(null);
        BillingWorkDto work = createBaseWork(100L, 10000);

        // When & Then
        assertThatThrownBy(() -> processor.process(work))
                .isExactlyInstanceOf(BillingFatalException.class)
                .hasMessageContaining("할인 정보 캐싱이 이루어지지 않았습니다");
    }

    @Test
    @DisplayName("2. 캐시에 없는 할인 ID가 들어오면 BillingException(DataNotFound) 발생")
    void process_DiscountIdMissingInCache_ThrowsException() {
        // Given
        setupMockCache(); // 기본 캐시 세팅 (ID: 1만 존재)

        // 캐시에 없는 할인 ID(99L)를 가진 데이터 생성
        BillingUserBillingInfoDto rawData = BillingUserBillingInfoDto.builder()
                .userId(1L)
                .discounts(List.of(UserSubscribeDiscountDto.builder().productId(1L).discountId(99L).build()))
                .build();
        BillingWorkDto work = BillingWorkDto.builder().rawData(rawData).build();

        // When & Then
        assertThatThrownBy(() -> processor.process(work))
                .isExactlyInstanceOf(BillingException.class)
                .hasMessageContaining("할인 정보(ID: 99)가 캐시에 없습니다");
    }

    @Test
    @DisplayName("3. 정률 할인(RATE)이 100%를 초과하면 invalidDiscountData 발생")
    void process_InvalidDiscountRate_ThrowsException() {
        // Given
        Map<Long, BillingDiscountDto> discountMap = new HashMap<>();
        discountMap.put(1L, BillingDiscountDto.builder()
                .discountId(1L).discountName("비정상할인").isCash(DiscountType.RATE).percent(150.0).build()); // 150% 할인

        setupMockProductCache();
        given(referenceCache.getDiscountMap()).willReturn(discountMap);

        BillingWorkDto work = createBaseWork(1L, 10000);
        work.getRawData().getDiscounts().add(UserSubscribeDiscountDto.builder().productId(1L).discountId(1L).build());

        // When & Then
        assertThatThrownBy(() -> processor.process(work))
                .isExactlyInstanceOf(BillingException.class)
                .hasFieldOrPropertyWithValue("errorCode", "ERR_INVALID_DISCOUNT_DATA");
    }

    @Test
    @DisplayName("4. 할인 금액이 원금보다 커도 최종 금액(totalPrice)은 0원 미만이 될 수 없음")
    void process_DiscountExceedsBaseAmount_ReturnsZero() throws Exception {
        // Given: 5,000원 상품에 10,000원 할인 적용
        Map<Long, BillingDiscountDto> discountMap = new HashMap<>();
        discountMap.put(1L, BillingDiscountDto.builder()
                .discountId(1L).discountName("대박할인").isCash(DiscountType.CASH).cash(10000).build());

        setupMockProductCache();
        given(referenceCache.getDiscountMap()).willReturn(discountMap);

        BillingWorkDto work = createBaseWork(1L, 5000); // 원금 5,000원
        work.getRawData().getDiscounts().add(UserSubscribeDiscountDto.builder().productId(1L).discountId(1L).build());

        // When
        BillingWorkDto result = processor.process(work);

        // Then
        assertThat(result.getDiscountAmount()).isEqualTo(10000);
        assertThat(result.getTotalPrice()).isEqualTo(0); // 음수가 아닌 0원 확인
    }

    // --- Helper Methods ---
    private void setupMockCache() {
        setupMockProductCache();
        Map<Long, BillingDiscountDto> discountMap = new HashMap<>();
        discountMap.put(1L, BillingDiscountDto.builder()
                .discountId(1L).discountName("기본할인").isCash(DiscountType.CASH).cash(1000).build());
        given(referenceCache.getDiscountMap()).willReturn(discountMap);
    }

    private void setupMockProductCache() {
        Map<Long, BillingProductDto> productMap = new HashMap<>();
        productMap.put(1L, BillingProductDto.builder()
                .productId(1L).price(10000).productName("상품").productType(ProductType.mobile).build());
        given(referenceCache.getProductMap()).willReturn(productMap);
    }

    private BillingWorkDto createBaseWork(Long userId, int baseAmount) {
        BillingUserBillingInfoDto rawData = BillingUserBillingInfoDto.builder()
                .userId(userId)
                .discounts(new java.util.ArrayList<>())
                .build();

        return BillingWorkDto.builder()
                .rawData(rawData)
                .baseAmount(baseAmount)
                .discounts(new java.util.ArrayList<>())
                .build();
    }
}
