package com.touplus.billing_api.domain.repository.billing;

import com.touplus.billing_api.domain.billing.entity.BillingProduct;

import java.util.List;

public interface BillingProductRepository {

    List<BillingProduct> findAll();

    BillingProduct findById(Long id);
}
