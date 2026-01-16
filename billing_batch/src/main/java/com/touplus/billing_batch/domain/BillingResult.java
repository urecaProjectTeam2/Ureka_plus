package com.touplus.billing_batch.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "billing_result",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_billing_month_user",
            columnNames = {"settlement_month", "user_id"}
        )
    }
)
@Getter
@NoArgsConstructor
public class BillingResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "billing_result_id")
    private Long id;

    @Column(name = "settlement_month", nullable = false)
    private LocalDate settlementMonth;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "total_price", nullable = false)
    private Integer totalPrice;

    @Column(name = "settlement_details", columnDefinition = "json", nullable = false)
    private String settlementDetails;

    @Enumerated(EnumType.STRING)
    @Column(name = "send_status")
    private SendStatus sendStatus = SendStatus.READY;

    @Column(name = "batch_execution_id", nullable = false)
    private Long batchExecutionId;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    /* ===== 상태 변경 ===== */

    public void markSending() {
        this.sendStatus = SendStatus.SENDING;
    }

    public void markSuccess() {
        this.sendStatus = SendStatus.SUCCESS;
    }

    public void markFail() {
        // this.sendStatus = SendStatus.FAIL;
    }
}
