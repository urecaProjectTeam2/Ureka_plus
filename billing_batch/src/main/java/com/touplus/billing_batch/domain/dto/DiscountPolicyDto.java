package com.touplus.billing_batch.domain.dto;

import com.touplus.billing_batch.domain.entity.DiscountPolicy;
import com.touplus.billing_batch.domain.enums.CalOrderType;
import com.touplus.billing_batch.domain.enums.DiscountRangeType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscountPolicyDto {

    private Long discountRangeId;
    private CalOrderType calOrder;
    private DiscountRangeType discountRange;

    // Entity -> DTO 변환
    public static DiscountPolicyDto fromEntity(DiscountPolicy entity) {
        if (entity == null) return null;
        return DiscountPolicyDto.builder()
                .discountRangeId(entity.getDiscountRangeId())
                .calOrder(entity.getCalOrder())
                .discountRange(entity.getDiscountRange())
                .build();
    }

    // DTO -> Entity 변환
    public DiscountPolicy toEntity() {
        return DiscountPolicy.builder()
                .discountRangeId(this.discountRangeId)
                .calOrder(this.calOrder)
                .discountRange(this.discountRange)
                .build();
    }
}
