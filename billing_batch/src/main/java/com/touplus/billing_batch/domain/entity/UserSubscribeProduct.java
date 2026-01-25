package com.touplus.billing_batch.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSubscribeProduct {

    @Id
    @Column(name = "user_subscribe_product_id", nullable = false)
    private Long userSubscribeProductId;

    @Column(name = "created_month", nullable = false)
    private LocalDate createdMonth;

    @Column(name = "deleted_at")
    private LocalDate deletedAt;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "product_id", nullable = false)
    private Long productId;
}
