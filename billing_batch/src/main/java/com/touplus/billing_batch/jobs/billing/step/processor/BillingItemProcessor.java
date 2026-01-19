package com.touplus.billing_batch.jobs.billing.step.processor;

import java.util.List;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.touplus.billing_batch.domain.dto.BillingCalculationResult;
import com.touplus.billing_batch.domain.entity.BillingProduct;
import com.touplus.billing_batch.domain.entity.BillingUser;
import com.touplus.billing_batch.domain.entity.UserSubscribeProduct;
import com.touplus.billing_batch.domain.repository.BillingProductRepository;
import com.touplus.billing_batch.domain.repository.UserSubscribeProductRepository;

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

        List<UserSubscribeProduct> subscriptions =
                uspRepository.findActiveByUserId(user.getUserId());

        List<BillingProduct> products = subscriptions.stream()
                .map(s -> productRepository.findById(s.getProductId()).orElseThrow())
                .toList();

        int totalPrice = products.stream()
                .mapToInt(BillingProduct::getPrice)
                .sum();

        return new BillingCalculationResult(
                user.getUserId(),
                totalPrice,
                products
        );
    }
}