package com.touplus.billing_batch.batch;

import com.touplus.billing_batch.domain.dto.AdditionalChargeDto;
import com.touplus.billing_batch.domain.dto.BillingProductDto;
import com.touplus.billing_batch.domain.dto.BillingUserBillingInfoDto;
import com.touplus.billing_batch.domain.dto.UserSubscribeProductDto;
import com.touplus.billing_batch.domain.enums.ProductType;
import com.touplus.billing_batch.jobs.billing.cache.BillingReferenceCache;
import com.touplus.billing_batch.jobs.billing.step.processor.AmountCalculationProcessor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class AmountCalculationProcessorLoadTest {

    @InjectMocks
    private AmountCalculationProcessor processor;

    @Mock
    private BillingReferenceCache referenceCache;

    @Test
    @DisplayName("대규모 데이터 처리 테스트 (유저 100만, 청구이력 500만)")
    void process_MassiveData_PerformanceTest() throws Exception {
        // 1. 사전 준비: 캐시 데이터 설정
        Map<Long, BillingProductDto> productMap = new HashMap<>();
        productMap.put(1L, BillingProductDto.builder()
                .productId(1L).price(10000).productName("기본상품").productType(ProductType.mobile).build());
        given(referenceCache.getProductMap()).willReturn(productMap);

        int userCount = 1_000_000;
        long totalChargeCount = 0;

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        System.out.println(">> 대규모 정산 프로세싱 테스트 시작 (User: 1M, Target Charges: 5M)...");

        for (int i = 0; i < userCount; i++) {
            // 유저당 평균 5건의 추가 요금 생성 (0~10건 랜덤)
            int chargeCountPerUser = i % 11;
            List<AdditionalChargeDto> charges = new ArrayList<>();
            for (int j = 0; j < chargeCountPerUser; j++) {
                charges.add(AdditionalChargeDto.builder()
                        .id((long) j)
                        .price(100).companyName("결제사" + j)
                        .build());
                totalChargeCount++;
            }

            BillingUserBillingInfoDto item = BillingUserBillingInfoDto.builder()
                    .userId((long) i)
                    .products(List.of(UserSubscribeProductDto.builder().productId(1L).build()))
                    .additionalCharges(charges)
                    .build();

            // 프로세싱 수행
            processor.process(item);

            // 진행률 출력 (printf를 사용하여 포맷 유지)
            if (i % 200_000 == 0) {
                System.out.printf(">> 진행률: %.1f%% (누적 처리 청구 건수: %d건)%n", (i / (double)userCount) * 100, totalChargeCount);
            }
        }

        stopWatch.stop();

        // 최종 결과 출력
        System.out.println(">> [테스트 결과]");
        System.out.println(">> 총 처리 유저: " + userCount + "명");
        System.out.println(">> 총 처리 청구 이력: " + totalChargeCount + "건");
        System.out.println(">> 소요 시간: " + stopWatch.getTotalTimeSeconds() + "초");
        System.out.println(">> 유저당 처리 속도: " + (stopWatch.getTotalTimeMillis() / (double)userCount) + "ms");
    }
}