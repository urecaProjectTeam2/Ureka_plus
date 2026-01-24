package com.touplus.billing_batch.domain.repository.service;

import com.touplus.billing_batch.domain.entity.DiscountPolicy;

import java.util.List;
import java.util.Optional;

public interface DiscountPolicyRepository {

    List<DiscountPolicy> findAll();
}
