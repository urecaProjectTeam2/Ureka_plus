package com.touplus.billing_batch.batch;

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
import org.springframework.util.StopWatch;

import java.util.*;

import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class DiscountCalculationProcessorTest {

    @InjectMocks
    private DiscountCalculationProcessor processor;

    @Mock(stubOnly = true)
    private BillingReferenceCache referenceCache;

    @Test
    @DisplayName("할인 처리 부하 테스트 (유저 100만, 할인이력 500만)")
    void process_MassiveDiscount_PerformanceTest() throws Exception {
        // 1. 사전 준비: 캐시 데이터 설정 (상품 1종, 할인 2종 - 정액/정률)
        Map<Long, BillingProductDto> productMap = new HashMap<>();
        productMap.put(1L, BillingProductDto.builder().productId(1L).price(50000).productType(ProductType.mobile).build());

        Map<Long, BillingDiscountDto> discountMap = new HashMap<>();
        discountMap.put(10L, BillingDiscountDto.builder().discountId(10L).discountName("정액할인").isCash(DiscountType.CASH).cash(1000).build());
        discountMap.put(20L, BillingDiscountDto.builder().discountId(20L).discountName("정률할인").isCash(DiscountType.RATE).percent(10.0).build());

        given(referenceCache.getProductMap()).willReturn(productMap);
        given(referenceCache.getDiscountMap()).willReturn(discountMap);

        int userCount = 1_000_000;
        long totalDiscountCount = 0;

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        System.out.println(">> 대규모 할인 정산 테스트 시작 (User: 1M, Target Discounts: 5M)...");

        for (int i = 0; i < userCount; i++) {
            // [최적화] 매 루프마다 고정된 리스트를 재사용하여 객체 생성을 최소화합니다.
            List<UserSubscribeDiscountDto> usdList = new ArrayList<>(5);
            for (int j = 0; j < 5; j++) {
                long discountId = (j % 2 == 0) ? 10L : 20L;
                usdList.add(UserSubscribeDiscountDto.builder().productId(1L).discountId(discountId).build());
                totalDiscountCount++;
            }

            BillingUserBillingInfoDto rawData = BillingUserBillingInfoDto.builder()
                    .userId((long) i)
                    .discounts(usdList)
                    .build();

            BillingWorkDto work = BillingWorkDto.builder()
                    .rawData(rawData)
                    .baseAmount(55000)
                    .discounts(new ArrayList<>(5)) // 상세 내역이 담길 리스트
                    .build();

            // 할인 금액 및 최종 금액 계산 수행
            processor.process(work);

            // 20만 건마다 진행률 및 메모리 정리 힌트 제공
            if (i % 200_000 == 0) {
                System.out.printf(">> 진행률: %.1f%% (누적 처리 할인 건수: %d건)%n", (i / (double)userCount) * 100, totalDiscountCount);
                // 대규모 루프 테스트에서만 선택적으로 사용 (GC 유도)
                // System.gc();
            }
        }

        stopWatch.stop();

        // 최종 결과 출력
        System.out.println("\n>> [할인 정산 테스트 결과]");
        System.out.println(">> 총 처리 유저: " + userCount + "명");
        System.out.println(">> 총 처리 할인 이력: " + totalDiscountCount + "건");
        System.out.println(">> 총 소요 시간: " + String.format("%.2f", stopWatch.getTotalTimeSeconds()) + "초");
        System.out.println(">> 초당 처리량(TPS): " + (int)(userCount / stopWatch.getTotalTimeSeconds()) + "건/sec");
        System.out.println(">> [END]");
    }
}
