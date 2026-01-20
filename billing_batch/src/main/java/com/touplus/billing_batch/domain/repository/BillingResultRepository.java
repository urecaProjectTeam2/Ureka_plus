package com.touplus.billing_batch.domain.repository;

import com.touplus.billing_batch.domain.entity.BillingResult;
import com.touplus.billing_batch.domain.enums.SendStatus;

import java.util.List;

public interface BillingResultRepository {

    List<BillingResult> findBySendStatusForUpdate(SendStatus status);
    void saveAll(List<BillingResult> results) throws Exception;
}
