package com.touplus.billing_batch.jobs.billing.step.processor;

import java.util.List;
import java.util.ArrayList;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.touplus.billing_batch.domain.dto.BillingCalculationResult;
import com.touplus.billing_batch.domain.entity.BillingProduct;
import com.touplus.billing_batch.domain.entity.BillingUser;
import com.touplus.billing_batch.domain.entity.UserSubscribeProduct;
import com.touplus.billing_batch.domain.repository.BillingProductRepository;
import com.touplus.billing_batch.domain.repository.UserSubscribeProductRepository;
import com.touplus.billing_batch.common.BillingException; // 커스텀 예외 임포트

@Component
public class BillingItemProcessor
        implements ItemProcessor<BillingUser, BillingCalculationResult> {

    private final UserSubscribeProductRepository uspRepository;
    private final BillingProductRepository productRepository;

    public BillingItemProcessor(
            UserSubscribeProductRepository uspRepository,
            BillingProductRepository productRepository) {
        this.uspRepository = uspRepository;
        this.productRepository = productRepository;
    }

    @Override
    public BillingCalculationResult process(BillingUser user) {

        // 1. 활성화된 구독 정보 조회
        List<UserSubscribeProduct> subscriptions =
                uspRepository.findActiveByUserId(user.getUserId());

        // [추가] 구독 중인 상품이 하나도 없는 경우 정산 대상에서 제외하거나 예외 발생
        if (subscriptions.isEmpty()) {
            throw BillingException.dataNotFound(user.getUserId());
        }

        // 2. 구독 정보 기반으로 상품 상세 정보 로드
        List<BillingProduct> products = new ArrayList<>();
        for (UserSubscribeProduct s : subscriptions) {
            BillingProduct product = productRepository.findById(s.getProductId())
                    .orElseThrow(() -> new BillingException(
                            "상품 정보를 찾을 수 없습니다. ProductID: " + s.getProductId(),
                            "ERR_PRODUCT_NOT_FOUND",
                            user.getUserId()
                    ));
            products.add(product);
        }

        // 3. 합계 금액 계산
        int totalPrice = products.stream()
                .mapToInt(BillingProduct::getPrice)
                .sum();

        // [추가] 합계 금액 검증 (예: 비즈니스 규칙상 0원 이하일 수 없는 경우)
        if (totalPrice < 0) {
            throw BillingException.invalidAmount(user.getUserId());
        }

        return new BillingCalculationResult(
                user.getUserId(),
                totalPrice,
                products
        );
    }
}