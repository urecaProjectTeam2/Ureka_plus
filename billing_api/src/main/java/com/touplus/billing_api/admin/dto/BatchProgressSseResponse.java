package com.touplus.billing_api.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class BatchProgressSseResponse {
    private String progress;             // 계산된 퍼센트 (예: "75.50%")
    private long totalProcessed;         // 현재까지 총 처리 건수
    private List<PartitionStatusDto> partitions; // 10개 파티션 각각의 상세 상태
    private boolean isCompleted;         // 배치가 완전히 끝났는지 여부
    private double tps;    // 초당 처리 건수
    private String etc;    // 예상 종료 시간
    private long skipCount;
    private List<BatchBillingErrorLogDto> recentErrors;
    private Long currentJobId;
}
