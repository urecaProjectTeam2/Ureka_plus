package com.touplus.billing_batch.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "user_subscribe_discount")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSubscribeDiscount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_discount_subscribe_id")
    private Long udsId;

    @Column(name = "discount_subscribe_month", nullable = false)
    private LocalDate subscribeMonth;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_usd_user"))
    private BillingUser billingUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discount_id", nullable = false, foreignKey = @ForeignKey(name = "fk_usd_discount"))
    private BillingDiscount billingDiscount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, foreignKey = @ForeignKey(name = "fk_usd_product"))
    private BillingProduct billingProduct;
}
