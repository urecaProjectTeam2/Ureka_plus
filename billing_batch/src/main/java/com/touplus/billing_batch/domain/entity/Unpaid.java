package com.touplus.billing_batch.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "unpaid", indexes = {
        @Index(name = "idx_unpaid_month_paid", columnList = "unpaid_month, is_paid"),
        @Index(name = "idx_unpaid_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Unpaid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "unpaid_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "unpaid_price", nullable = false)
    private Integer unpaidPrice;

    @Column(name = "unpaid_month", nullable = false)
    private LocalDate unpaidMonth;

    @Column(name = "is_paid", nullable = false)
    private Boolean paid = false;
}
