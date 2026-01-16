package com.touplus.billing_batch.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_subscribe_product")
@Getter
@NoArgsConstructor
public class UserSubscribeProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userSubscribeProductId;

    private LocalDate createdMonth;

    private LocalDateTime deletedAt;

    private Long userId;
    private Long productId;
}