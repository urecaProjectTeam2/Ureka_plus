package com.touplus.billing_batch.domain.repository;

import com.touplus.billing_batch.domain.entity.UserSubscribeProduct;

import java.time.LocalDate;
import java.util.List;

public interface UserSubscribeProductRepository {

    /**
     * JPA:
     * findActiveByUserId(Long userId)
     */
    List<UserSubscribeProduct> findActiveByUserId(Long userId);

    /**
     * JPA:
     * findByUserIdIn(List<Long> userIds)
     */
    List<UserSubscribeProduct> findByUserIdIn(List<Long> userIds, LocalDate startDate, LocalDate endDate);
}
