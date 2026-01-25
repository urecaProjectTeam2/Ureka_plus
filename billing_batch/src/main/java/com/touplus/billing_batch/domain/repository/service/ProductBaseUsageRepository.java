package com.touplus.billing_batch.domain.repository.service;

import com.touplus.billing_batch.domain.dto.ProductBaseUsageDto;
import com.touplus.billing_batch.domain.entity.ProductBaseUsage;
import java.util.List;

public interface ProductBaseUsageRepository {
    List<ProductBaseUsage> findByProductId(Long productId);
    List<ProductBaseUsage> findAll();
}