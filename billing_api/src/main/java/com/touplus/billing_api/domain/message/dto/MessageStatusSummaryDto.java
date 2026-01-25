package com.touplus.billing_api.domain.message.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MessageStatusSummaryDto {
    private long waitCount;
    private long retryCount;
    private long sentCount;
    private long smsCount;
    private long failCount;
    private long totalCount;
    
    private double waitRate;
    private double retryRate;
    private double sentRate;
    private double failRate;
    private double smsRate;
    
    private String settlementMonth;
}

