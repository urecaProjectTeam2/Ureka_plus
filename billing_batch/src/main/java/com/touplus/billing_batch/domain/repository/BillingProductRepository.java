package com.touplus.billing_batch.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.touplus.billing_batch.domain.entity.BillingProduct;

public interface BillingProductRepository extends JpaRepository<BillingProduct, Long> {
}