package com.touplus.billing_batch.domain.entity;

import com.touplus.billing_batch.domain.enums.ProductType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "billing_product")
@Getter
@NoArgsConstructor
public class BillingProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;

    private String productName;

    @Enumerated(EnumType.STRING)
    private ProductType productType;

    private int price;
}