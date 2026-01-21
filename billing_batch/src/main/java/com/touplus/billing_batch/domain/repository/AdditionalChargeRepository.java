package com.touplus.billing_batch.domain.repository;

import com.touplus.billing_batch.domain.entity.AdditionalCharge;
import com.touplus.billing_batch.domain.entity.BillingUser;

import java.time.LocalDate;
import java.util.List;

public interface AdditionalChargeRepository {

    List<AdditionalCharge> findByUser(BillingUser user);

    List<AdditionalCharge> findByAdditionalChargeMonth(LocalDate month);

    List<AdditionalCharge> findByUserIdIn(List<Long> userIds, LocalDate startDate, LocalDate endDate);
}