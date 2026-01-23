package com.touplus.billing_api.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MessageWithSettlementMonthDto {
    private Long messageId;
    private Long billingId;
    private String messageStatus;
    private String settlementMonth;
    private String content;
}
