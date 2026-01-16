package com.touplus.billing_batch.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.touplus.billing_batch.domain.entity.BillingUser;

public interface BillingUserRepository extends JpaRepository<BillingUser, Long> {
}

