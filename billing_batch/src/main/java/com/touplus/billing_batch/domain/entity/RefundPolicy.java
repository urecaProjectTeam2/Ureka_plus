package com.touplus.billing_batch.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundPolicy {

    @Id
    @Column(name = "refund_policy_id", nullable = false)
    private Long refundPolicyId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "refund_duration", nullable = false)
    private Integer refundDuration;

}