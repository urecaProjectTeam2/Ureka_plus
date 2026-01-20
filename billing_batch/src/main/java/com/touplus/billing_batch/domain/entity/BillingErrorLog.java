package com.touplus.billing_batch.domain.entity;

import com.touplus.billing_batch.domain.enums.ErrorType;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "batch_billing_error_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class BillingErrorLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "error_log_id")
    private Long id;

    private Long jobExecutionId;
    private Long stepExecutionId;
    private String jobName;
    private String stepName;
    private LocalDate settlementMonth;
    private Long userId;

    @Enumerated(EnumType.STRING)
    private ErrorType errorType;

    private String errorCode;
    private String errorMessage;

    @Builder.Default
    private boolean resolved = true;

    @Builder.Default
    private boolean processed = false;

    @Builder.Default
    private LocalDateTime occurredAt = LocalDateTime.now();

    // 처리 완료 상태 변경 메서드
    public void markAsProcessed() {
        this.processed = true;
    }
}