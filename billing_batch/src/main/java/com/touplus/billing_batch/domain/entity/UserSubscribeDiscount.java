package com.touplus.billing_batch.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSubscribeDiscount {

    @Id
    @Column(name = "user_discount_subscribe_id", nullable = false)
    private Long udsId;

    @Column(name = "discount_subscribe_month", nullable = false)
    private LocalDate discountSubscribeMonth;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "discount_id", nullable = false)
    private Long discountId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
