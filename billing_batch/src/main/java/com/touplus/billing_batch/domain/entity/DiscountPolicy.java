package com.touplus.billing_batch.domain.entity;

import com.touplus.billing_batch.domain.enums.CalOrderType;
import com.touplus.billing_batch.domain.enums.DiscountRangeType;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscountPolicy {

    @Id
    @Column(name = "discount_range_id", nullable = false)
    private Long discountRangeId;

    @Column(name="cal_order", nullable = false)
    private CalOrderType calOrder;

    @Column(name="discount_range", nullable = false)
    private DiscountRangeType discountRange;

}
