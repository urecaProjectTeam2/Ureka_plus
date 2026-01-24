package com.touplus.billing_batch.domain.dto;

import com.touplus.billing_batch.domain.entity.ProductBaseUsage;
import com.touplus.billing_batch.domain.enums.UseType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductBaseUsageDto {
    private Long productBaseUsageId;
    private Long productId;
    private Long overusePolicyId;
    private UseType useType;
    private Integer basicAmount;

    /**
     * Entity -> DTO 변환
     */
    public static ProductBaseUsageDto fromEntity(ProductBaseUsage entity) {
        return ProductBaseUsageDto.builder()
                .productBaseUsageId(entity.getProductBaseUsageId())
                .productId(entity.getProductId())
                .overusePolicyId(entity.getOverusePolicyId())
                .useType(entity.getUseType())
                .basicAmount(entity.getBasicAmount())
                .build();
    }

    /**
     * DTO -> Entity 변환
     */
    public ProductBaseUsage toEntity() {
        return ProductBaseUsage.builder()
                .productBaseUsageId(this.productBaseUsageId)
                .productId(this.productId)
                .overusePolicyId(this.overusePolicyId)
                .useType(this.useType)
                .basicAmount(this.basicAmount)
                .build();
    }
}