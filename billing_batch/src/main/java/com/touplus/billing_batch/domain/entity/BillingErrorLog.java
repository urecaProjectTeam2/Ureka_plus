package com.touplus.billing_batch.domain.entity;

import com.touplus.billing_batch.domain.enums.ErrorType;
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
public class BillingErrorLog {

    @Id
    @Column(name = "error_log_id", nullable = false)
    private Long id;

    @Column(name = "job_execution_id")
    private Long jobExecutionId;

    @Column(name = "step_execution_id")
    private Long stepExecutionId;

    @Column(name = "job_name")
    private String jobName;

    @Column(name = "step_name")
    private String stepName;

    @Column(name = "settlement_month")
    private LocalDate settlementMonth;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "error_type")
    private ErrorType errorType;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "is_recoverable")
    private boolean isRecoverable = true;

    @Column(name = "processed")
    private boolean processed = false;

    @Column(name = "occurred_at")
    private LocalDateTime occurredAt = LocalDateTime.now();

    // 처리 완료 상태 변경 메서드
    public void markAsProcessed() {
        this.processed = true;
    }
}
