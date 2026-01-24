package com.touplus.billing_batch.domain.repository;

import com.touplus.billing_batch.domain.entity.MvnoDiscountPolicy;

import java.util.List;

public interface MvnoDiscountPolicyRepository {
    List<MvnoDiscountPolicy> findByProductId(Long productId);
}
