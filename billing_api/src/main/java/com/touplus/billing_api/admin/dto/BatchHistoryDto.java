package com.touplus.billing_api.admin.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchHistoryDto {
    private Long executionId;      // 배치 실행 ID
    private String status;         // 상태 (COMPLETED, FAILED 등)
    private LocalDateTime startTime;   // 시작 시간
    private LocalDateTime endTime;     // 종료 시간
    private long duration;         // 소요 시간 (초)
    private long totalWrite;       // 총 처리 건수
    private long totalAmount;      // 총 정산 금액
    private String exitCode;

    private long skipCount;
}
