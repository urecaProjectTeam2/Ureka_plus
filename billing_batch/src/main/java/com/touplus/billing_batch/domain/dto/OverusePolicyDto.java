package com.touplus.billing_batch.domain.dto;

import com.touplus.billing_batch.domain.entity.OverusePolicy;
import com.touplus.billing_batch.domain.enums.UseType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OverusePolicyDto {
    private Long overusePolicyId;
    private UseType useType;
    private Integer unitPrice;

    /**
     * Entity -> DTO 변환
     */
    public static OverusePolicyDto fromEntity(OverusePolicy entity) {
        return OverusePolicyDto.builder()
                .overusePolicyId(entity.getOverusePolicyId())
                .useType(entity.getUseType())
                .unitPrice(entity.getUnitPrice())
                .build();
    }

    /**
     * DTO -> Entity 변환
     */
    public OverusePolicy toEntity() {
        return OverusePolicy.builder()
                .overusePolicyId(this.overusePolicyId)
                .useType(this.useType)
                .unitPrice(this.unitPrice)
                .build();
    }
}