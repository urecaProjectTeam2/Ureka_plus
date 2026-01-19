package com.touplus.billing_batch.domain.repository;

import com.touplus.billing_batch.domain.entity.BillingResult;
import com.touplus.billing_batch.domain.enums.SendStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;

public interface BillingResultRepository extends JpaRepository<BillingResult, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<BillingResult> findBySendStatusOrderById(SendStatus status);
}
