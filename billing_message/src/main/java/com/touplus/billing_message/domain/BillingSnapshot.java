package com.touplus.billing_message.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(
    name = "billing_snapshot",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_user_month",
            columnNames = {"user_id", "settlement_month"}
        )
    }
)
@Getter
@NoArgsConstructor
public class BillingSnapshot {

    @Id
    @Column(name = "billing_id")
    private Long billingId; // Batch에서 생성된 ID

    @Column(name = "settlement_month", nullable = false)
    private LocalDate settlementMonth;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "total_price", nullable = false)
    private Integer totalPrice;

    @Column(name = "settlement_details", columnDefinition = "json", nullable = false)
    private String settlementDetails;

    public BillingSnapshot(
            Long billingId,
            LocalDate settlementMonth,
            Long userId,
            Integer totalPrice,
            String settlementDetails
    ) {
        this.billingId = billingId;
        this.settlementMonth = settlementMonth;
        this.userId = userId;
        this.totalPrice = totalPrice;
        this.settlementDetails = settlementDetails;
    }
}
