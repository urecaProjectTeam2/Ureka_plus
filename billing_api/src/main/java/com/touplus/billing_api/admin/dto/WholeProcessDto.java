package com.touplus.billing_api.admin.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WholeProcessDto {

    private long batchCount;
    private long kafkaSentCount;
    private long kafkaReceiveCount;
    private long createMessageCount;
    private long sentMessageCount;
    private long totalCount;
    
    private double batchRate;
    private double kafkaSentRate;
    private double kafkaReceiveRate;
    private double createMessageRate;
    private double sentMessageRate;
    
    private String settlementMonth;

}
