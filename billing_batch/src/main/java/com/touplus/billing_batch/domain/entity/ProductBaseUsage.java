package com.touplus.billing_batch.domain.entity;

import com.touplus.billing_batch.domain.enums.UseType;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductBaseUsage {

    @Id
    @Column(name = "product_base_usage_id", nullable = false)
    private Long productBaseUsageId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "overuse_policy_id", nullable = false)
    private Long overusePolicyId; // overuse_policy 테이블 참조

    @Column(name = "use_type", nullable = false)
    private UseType useType;

    @Column(name = "basic_amount", nullable = false)
    private Integer basicAmount;

}