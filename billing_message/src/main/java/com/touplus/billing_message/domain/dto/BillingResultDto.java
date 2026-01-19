package com.touplus.billing_message.domain.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class BillingResultDto {
    private Long id;
    private LocalDate settlementMonth;
    private Long userId;
    private Integer totalPrice;
    private String settlementDetails;
    private String sendStatus;
    private Long batchExecutionId;
    private LocalDateTime processedAt;
}