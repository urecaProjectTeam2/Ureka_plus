package com.touplus.billing_batch.domain.repository;

import com.touplus.billing_batch.domain.entity.BillingActionLog;
import com.touplus.billing_batch.domain.entity.BillingErrorLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingActionLogRepository extends JpaRepository<BillingActionLog, Long> {
}
