package com.touplus.billing_batch.domain.entity;

import com.touplus.billing_batch.domain.enums.ProductType;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingProduct {

    @Id
    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "product_type")
    private ProductType productType;

    @Column(name = "price")
    private Integer price;
}