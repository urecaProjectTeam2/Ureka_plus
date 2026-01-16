package com.touplus.billing_message.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingSnapshotRepository
        extends JpaRepository<BillingSnapshot, Long> {
}
