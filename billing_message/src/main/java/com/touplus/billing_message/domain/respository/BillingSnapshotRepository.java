package com.touplus.billing_message.domain.respository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.touplus.billing_message.domain.entity.BillingSnapshot;

public interface BillingSnapshotRepository
        extends JpaRepository<BillingSnapshot, Long> {
}
