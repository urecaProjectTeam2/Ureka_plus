package com.touplus.billing_api.domain.repository.billing;

import com.touplus.billing_api.admin.dto.BillingProductStatResponse;
import com.touplus.billing_api.domain.billing.entity.BillingProduct;

import java.util.List;

public interface BillingProductRepository {

    List<BillingProduct> findAll();

    BillingProduct findById(Long id);

    List<BillingProductStatResponse> findTopSubscribedByProductType(
            List<String> productTypes, int limit
    );
}
