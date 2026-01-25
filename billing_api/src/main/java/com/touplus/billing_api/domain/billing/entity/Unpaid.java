package com.touplus.billing_api.domain.billing.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Unpaid {

    @Id
    @Column(name = "unpaid_id", nullable = false)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "unpaid_price", nullable = false)
    private Integer unpaidPrice;

    @Column(name = "unpaid_month", nullable = false)
    private LocalDate unpaidMonth;

    @Column(name = "is_paid", nullable = false)
    @Builder.Default
    private Boolean paid = false;
}
