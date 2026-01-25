package com.touplus.billing_batch.domain.repository.service;

import com.touplus.billing_batch.domain.entity.OverusePolicy;
import java.util.List;
import java.util.Optional;

public interface OverusePolicyRepository {
    Optional<OverusePolicy> findById(Long id);
    List<OverusePolicy> findAll();
}