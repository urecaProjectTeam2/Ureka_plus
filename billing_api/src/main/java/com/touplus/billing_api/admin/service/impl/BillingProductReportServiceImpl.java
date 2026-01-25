package com.touplus.billing_api.admin.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.touplus.billing_api.admin.dto.BillingProductStatResponse;
import com.touplus.billing_api.admin.service.BillingProductReportService;
import com.touplus.billing_api.domain.repository.billing.BillingProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BillingProductReportServiceImpl implements BillingProductReportService {

    private final BillingProductRepository billingProductRepository;

    @Override
    public List<BillingProductStatResponse> getTopSubscribedProducts(
            List<String> productTypes, int limit
    ) {
        return billingProductRepository
                .findTopSubscribedByProductType(productTypes, limit);
    }
}

