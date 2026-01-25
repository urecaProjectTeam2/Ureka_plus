package com.touplus.billing_batch.domain.entity;

import com.touplus.billing_batch.domain.enums.SendStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingResult {

    @Id
    @Column(name = "billing_result_id", nullable = false)
    private Long id;

    @Column(name = "settlement_month", nullable = false)
    private LocalDate settlementMonth;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "total_price", nullable = false)
    private Integer totalPrice;

    @Column(name = "settlement_details", nullable = false)
    private String settlementDetails; // JSON 컬럼은 JDBC에서 String으로 처리

    @Column(name = "send_status")
    @Builder.Default
    private SendStatus sendStatus = SendStatus.READY;

    @Column(name = "batch_execution_id", nullable = false)
    private Long batchExecutionId;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    /* ===== 상태 변경 메서드 ===== */
    public void markSending() {
        this.sendStatus = SendStatus.SENDING;
    }

    public void markSuccess() {
        this.sendStatus = SendStatus.SUCCESS;
    }

    public void markFail() {
        this.sendStatus = SendStatus.FAIL;
    }
}