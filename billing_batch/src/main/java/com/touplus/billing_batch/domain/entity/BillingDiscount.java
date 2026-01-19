package com.touplus.billing_batch.domain.entity;

import com.touplus.billing_batch.domain.enums.DiscountType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "billing_discount")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingDiscount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="discount_id")
    private Long discountId;

    @Column(name="discount_name", length = 50, nullable = false)
    private String discountName;

    @Column(name="is_cash", nullable = false)
    private DiscountType isCash;

    @Column(name = "cash")
    private Integer cash;

    @Column(name = "percent")
    private Double percent;

}
