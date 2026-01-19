package com.touplus.billing_batch.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_subscribe_product", indexes = {
        @Index(name = "idx_usp_user_active", columnList = "user_id, deleted_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSubscribeProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userSubscribeProductId;

    @Column(name = "created_month", nullable = false)
    private LocalDate createdMonth;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ===== FK 연관관계 =====
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_usp_user"))
    private BillingUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, foreignKey = @ForeignKey(name = "fk_usp_product"))
    private BillingProduct product;
}