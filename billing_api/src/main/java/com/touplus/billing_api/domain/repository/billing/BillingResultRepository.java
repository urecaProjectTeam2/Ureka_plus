package com.touplus.billing_api.domain.repository.billing;

import com.touplus.billing_api.domain.billing.entity.BillingResult;
import com.touplus.billing_api.domain.billing.enums.SendStatus;

import java.time.LocalDate;
import java.util.List;

public interface BillingResultRepository {


    List<BillingResult> findByUserIdsAndMonth(List<Long> userIds, LocalDate settlementMonth);
}
