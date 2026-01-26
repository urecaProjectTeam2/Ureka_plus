package com.touplus.billing_api.admin.dto;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchBillingErrorLogDto {

    private Long errorLogId;          // error_log_id (PK)
    private Long jobExecutionId;      // job_execution_id (FK)
    private Long stepExecutionId;     // step_execution_id
    private String jobName;           // job_name (Batch Job 이름)
    private String stepName;          // step_name (Batch Step 이름)
    private LocalDate settlementMonth; // settlement_month (정산월)
    private Long userId;              // user_id (에러 대상 사용자)

    private String errorType;         // error_type ('DATA', 'SYSTEM', 'NETWORK')

    private String errorCode;         // error_code (커스텀 에러 코드)
    private String errorMessage;      // error_message (에러 상세 내용)

    private boolean resolved;         // resolved (해결 여부, tinyint 1)
    private int processed;            // processed (처리 상태, tinyint)
    private LocalDateTime occurredAt;  // occurred_at (에러 발생 시각)

}