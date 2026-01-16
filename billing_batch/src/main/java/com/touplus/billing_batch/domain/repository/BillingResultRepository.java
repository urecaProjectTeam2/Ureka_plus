package com.touplus.billing_batch.domain.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.touplus.billing_batch.domain.entity.BillingResult;
import com.touplus.billing_batch.domain.entity.SendStatus;

import jakarta.persistence.LockModeType;
import java.util.List;

public interface BillingResultRepository extends JpaRepository<BillingResult, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<BillingResult> findBySendStatusOrderById(SendStatus status);
}
