package com.touplus.billing_batch.domain.entity;

import com.touplus.billing_batch.domain.enums.DiscountType;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingDiscount {

    @Id
    @Column(name = "discount_id", nullable = false)
    private Long discountId;

    @Column(name = "discount_name", length = 50, nullable = false)
    private String discountName;

    @Column(name = "is_cash", nullable = false)
    private DiscountType isCash;

    @Column(name = "cash")
    private Integer cash;

    @Column(name = "percent")
    private Double percent;
}
