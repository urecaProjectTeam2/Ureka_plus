package com.touplus.billing_batch.domain.repository.service;

import com.touplus.billing_batch.domain.entity.RefundPolicy;

import java.util.List;
import java.util.Optional;

public interface RefundPolicyRepository {
    Optional<RefundPolicy> findByProductId(Long productId);
    List<RefundPolicy> findAll();
}