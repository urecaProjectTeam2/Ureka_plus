package com.touplus.billing_batch.domain.dto;

import com.touplus.billing_batch.domain.entity.MvnoDiscountPolicy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MvnoDiscountPolicyDto {
    private Long mvnoDiscountPolicyId;
    private Long productId;
    private Integer discountDuration;
    private Integer discountAmount;

    /**
     * Entity -> DTO 변환
     */
    public static MvnoDiscountPolicyDto fromEntity(MvnoDiscountPolicy entity) {
        return MvnoDiscountPolicyDto.builder()
                .mvnoDiscountPolicyId(entity.getMvnoDiscountPolicyId())
                .productId(entity.getProductId())
                .discountDuration(entity.getDiscountDuration())
                .discountAmount(entity.getDiscountAmount())
                .build();
    }

    /**
     * DTO -> Entity 변환
     */
    public MvnoDiscountPolicy toEntity() {
        return MvnoDiscountPolicy.builder()
                .mvnoDiscountPolicyId(this.mvnoDiscountPolicyId)
                .productId(this.productId)
                .discountDuration(this.discountDuration)
                .discountAmount(this.discountAmount)
                .build();
    }
}