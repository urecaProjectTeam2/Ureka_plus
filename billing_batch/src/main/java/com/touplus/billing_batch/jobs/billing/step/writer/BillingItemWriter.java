package com.touplus.billing_batch.jobs.billing.step.writer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.touplus.billing_batch.domain.entity.*;
import com.touplus.billing_batch.domain.repository.*;

@Component
public class BillingItemWriter
        implements ItemWriter<BillingUser> {

    private final UserSubscribeProductRepository uspRepository;
    private final BillingProductRepository productRepository;
    private final BillingResultRepository resultRepository;
    private final ObjectMapper objectMapper;

    public BillingItemWriter(
            UserSubscribeProductRepository uspRepository,
            BillingProductRepository productRepository,
            BillingResultRepository resultRepository,
            ObjectMapper objectMapper
    ) {
        this.uspRepository = uspRepository;
        this.productRepository = productRepository;
        this.resultRepository = resultRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void write(Chunk<? extends BillingUser> users) throws Exception {

        if (users.isEmpty()) return;

        /* 1. 유저 ID 수집 */
        List<Long> userIds = users.stream()
                .map(BillingUser::getUserId)
                .toList();

        /* 2. 이용내역 (청크당 1쿼리) */
        List<UserSubscribeProduct> subscriptions =
                uspRepository.findActiveByUserIds(userIds);

        if (subscriptions.isEmpty()) {
            return;
        }

        /* 3. 상품 일괄 조회 (청크당 1쿼리) */
        List<Long> productIds = subscriptions.stream()
                .map(UserSubscribeProduct::getProductId)
                .distinct()
                .toList();

        Map<Long, BillingProduct> productMap =
                productRepository.findByIdIn(productIds)
                        .stream()
                        .collect(Collectors.toMap(
                                BillingProduct::getProductId,
                                p -> p
                        ));

        /* 4. 유저별 상품 매핑 */
        Map<Long, List<BillingProduct>> userProducts = new HashMap<>();

        for (UserSubscribeProduct sub : subscriptions) {
            BillingProduct product = productMap.get(sub.getProductId());
            if (product == null) continue;

            userProducts
                    .computeIfAbsent(sub.getUserId(), k -> new ArrayList<>())
                    .add(product);
        }

        /* 5. BillingResult 생성 */
        List<BillingResult> results = new ArrayList<>();

        for (BillingUser user : users) {
            List<BillingProduct> products =
                    userProducts.getOrDefault(user.getUserId(), List.of());

            int totalPrice = products.stream()
                    .mapToInt(BillingProduct::getPrice)
                    .sum();

            BillingResult result = BillingResult.builder()
                    .userId(user.getUserId())
                    .settlementMonth(LocalDate.now().withDayOfMonth(1))
                    .totalPrice(totalPrice)
                    .settlementDetails(
                            objectMapper.writeValueAsString(products)
                    )
                    .sendStatus(SendStatus.READY)
                    .batchExecutionId(0L)
                    .processedAt(LocalDateTime.now())
                    .build();

            results.add(result);
        }

        /* 6. 일괄 저장 */
        resultRepository.saveAll(results);
    }
}
