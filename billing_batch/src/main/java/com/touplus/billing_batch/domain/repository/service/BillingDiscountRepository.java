package com.touplus.billing_batch.domain.repository.service;

import com.touplus.billing_batch.domain.entity.BillingDiscount;

import java.util.List;

public interface BillingDiscountRepository {

    List<BillingDiscount> findAll();
    BillingDiscount findById(Long id);
}
