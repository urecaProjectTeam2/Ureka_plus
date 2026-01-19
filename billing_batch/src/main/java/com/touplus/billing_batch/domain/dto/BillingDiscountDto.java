package com.touplus.billing_batch.domain.dto;

import com.touplus.billing_batch.domain.enums.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingDiscountDto {
    private Long discountId;
    private String discountName;
    private DiscountType isCash;
    private Integer cash;
    private Double percent;
}
