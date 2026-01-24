package com.touplus.billing_batch.domain.dto;

import com.touplus.billing_batch.domain.entity.RefundPolicy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundPolicyDto {
    private Long refundPolicyId;
    private Long productId;
    private Integer refundDuration;

    /**
     * Entity -> DTO 변환
     */
    public static RefundPolicyDto fromEntity(RefundPolicy entity) {
        return RefundPolicyDto.builder()
                .refundPolicyId(entity.getRefundPolicyId())
                .productId(entity.getProductId())
                .refundDuration(entity.getRefundDuration())
                .build();
    }

    /**
     * DTO -> Entity 변환
     */
    public RefundPolicy toEntity() {
        return RefundPolicy.builder()
                .refundPolicyId(this.refundPolicyId)
                .productId(this.productId)
                .refundDuration(this.refundDuration)
                .build();
    }
}