package com.touplus.billing_batch.domain.repository;

import com.touplus.billing_batch.domain.entity.DiscountPolicy;

import java.util.Optional;

public interface DiscountPolicyRepository {
    Optional<DiscountPolicy> findByDiscountRangeId(Long discountRangeId);
}
