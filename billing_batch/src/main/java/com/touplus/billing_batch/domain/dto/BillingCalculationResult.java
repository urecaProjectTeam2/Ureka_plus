package com.touplus.billing_batch.domain.dto;

import java.util.List;

import com.touplus.billing_batch.domain.entity.BillingProduct;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BillingCalculationResult {

    private Long userId;
    private int totalPrice;
    private List<BillingProduct> products;
}
