package com.touplus.billing_batch.domain.repository;

import com.touplus.billing_batch.domain.entity.BillingErrorLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingErrorLogRepository extends JpaRepository<BillingErrorLog,Long> {
}
