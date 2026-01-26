package com.touplus.billing_api.admin.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartitionStatusDto {
    private String stepName;    // 파티션 명 (partition0, 1...)
    private String status;      // 현재 파티션 상태
    private long readCount;     // 읽은 건수
    private long writeCount;    // 저장 성공 건수
    private long commitCount;   // 커밋 횟수
    private long skipCount;
}
