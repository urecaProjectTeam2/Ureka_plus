package com.touplus.billing_batch.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingUser {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "group_id")
    private Long groupId;
}