package com.touplus.billing_batch.domain.entity;

import com.touplus.billing_batch.domain.enums.UseType;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OverusePolicy {

    @Id
    @Column(name = "overuse_policy_id", nullable = false)
    private Long overusePolicyId;

    @Column(name = "use_type", nullable = false)
    private UseType useType;

    @Column(name = "unit_price", nullable = false)
    private Integer unitPrice;

}