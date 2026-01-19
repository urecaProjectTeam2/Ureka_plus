package com.touplus.billing_batch.domain.repository;

import com.touplus.billing_batch.domain.entity.BillingDiscount;
import com.touplus.billing_batch.domain.entity.UserSubscribeDiscount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSubscribeDiscountRepository extends JpaRepository<UserSubscribeDiscount, Long> {
}
