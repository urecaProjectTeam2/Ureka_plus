package com.touplus.billing_batch.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdditionalCharge {

    @Id
    @Column(name = "additional_charge_id")
    private Long id;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "price", nullable = false)
    private Integer price;

    @Column(name = "additional_charge_month", nullable = false)
    private LocalDate additionalChargeMonth;

    @Column(name = "user_id", nullable = false)
    private Long userId;
}