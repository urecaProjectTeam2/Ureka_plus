package com.touplus.billing_batch.domain.dto;

import com.touplus.billing_batch.domain.entity.BillingResult;
import com.touplus.billing_batch.domain.enums.SendStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingResultDto {

    private Long id;
    private LocalDate settlementMonth;
    private Long userId;
    private Integer totalPrice;
    private String settlementDetails;
    private SendStatus sendStatus;
    private Long batchExecutionId;
    private LocalDateTime processedAt;

    // Entity -> DTO
    public static BillingResultDto fromEntity(BillingResult entity) {
        if (entity == null) return null;

        return BillingResultDto.builder()
                .id(entity.getId())
                .settlementMonth(entity.getSettlementMonth())
                .userId(entity.getUserId())
                .totalPrice(entity.getTotalPrice())
                .settlementDetails(entity.getSettlementDetails())
                .sendStatus(entity.getSendStatus())
                .batchExecutionId(entity.getBatchExecutionId())
                .processedAt(entity.getProcessedAt())
                .build();
    }

    // DTO -> Entity
    public BillingResult toEntity() {
        return BillingResult.builder()
                .id(this.id)
                .settlementMonth(this.settlementMonth)
                .userId(this.userId)
                .totalPrice(this.totalPrice)
                .settlementDetails(this.settlementDetails)
                .sendStatus(this.sendStatus)
                .batchExecutionId(this.batchExecutionId)
                .processedAt(this.processedAt)
                .build();
    }
}