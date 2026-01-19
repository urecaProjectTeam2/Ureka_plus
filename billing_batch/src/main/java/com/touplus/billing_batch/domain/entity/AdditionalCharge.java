package com.touplus.billing_batch.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "additional_charge", indexes = {
        @Index(name = "idx_additional_user_month", columnList = "user_id, additional_charge_month")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdditionalCharge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "additional_charge_id")
    private Long id;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "price", nullable = false)
    private Integer price;

    @Column(name = "additional_charge_month", nullable = false)
    private LocalDate additionalChargeMonth;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_additional_user"))
    private BillingUser user;
}