package com.touplus.billing_api.domain.billing.dto;

import com.touplus.billing_api.domain.billing.entity.BillingProduct;

import com.touplus.billing_api.domain.billing.enums.ProductType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingProductDto {
    private Long productId;
    private String productName;
    private ProductType productType;
    private Integer price;

    public static BillingProductDto fromEntity(BillingProduct entity) {
        return BillingProductDto.builder()
                .productId(entity.getProductId())
                .productName(entity.getProductName())
                .productType(entity.getProductType())
                .price(entity.getPrice())
                .build();
    }

    public BillingProduct toEntity() {
        return BillingProduct.builder()
                .productId(this.productId)
                .productName(this.productName)
                .productType(this.productType)
                .price(this.price)
                .build();
    }
}