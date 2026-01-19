package com.touplus.billing_batch.domain.repository;

import com.touplus.billing_batch.domain.entity.BillingDiscount;
import com.touplus.billing_batch.domain.entity.BillingProduct;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingDiscountRepository extends JpaRepository<BillingDiscount, Long> {
}
