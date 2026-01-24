package com.touplus.billing_batch.domain.dto;

import com.touplus.billing_batch.domain.entity.BillingProduct;
import com.touplus.billing_batch.domain.enums.Network_type;
import com.touplus.billing_batch.domain.enums.ProductType;
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
    private Network_type networkType;

    public static BillingProductDto fromEntity(BillingProduct entity) {
        return BillingProductDto.builder()
                .productId(entity.getProductId())
                .productName(entity.getProductName())
                .productType(entity.getProductType())
                .price(entity.getPrice())
                .networkType(entity.getNetworkType())
                .build();
    }

    public BillingProduct toEntity() {
        return BillingProduct.builder()
                .productId(this.productId)
                .productName(this.productName)
                .productType(this.productType)
                .price(this.price)
                .networkType(this.networkType)
                .build();
    }
}