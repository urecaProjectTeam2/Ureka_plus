package com.touplus.billing_batch.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MvnoDiscountPolicy {

    @Id
    @Column(name = "mvno_discount_policy_id", nullable = false)
    private Long mvnoDiscountPolicyId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "discount_duration", nullable = false)
    private Integer discountDuration;

    @Column(name = "discount_amount", nullable = false)
    private Integer discountAmount;
}
