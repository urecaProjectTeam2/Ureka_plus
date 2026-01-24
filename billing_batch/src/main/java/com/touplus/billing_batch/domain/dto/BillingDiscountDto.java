package com.touplus.billing_batch.domain.dto;

import com.touplus.billing_batch.domain.entity.BillingDiscount;
import com.touplus.billing_batch.domain.enums.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingDiscountDto {
    private Long discountId;
    private String discountName;
    private DiscountType isCash;
    private Integer cash;
    private Double percent;
    private Integer value;

    // Entity -> DTO
    public static BillingDiscountDto fromEntity(BillingDiscount entity) {
        return BillingDiscountDto.builder()
                .discountId(entity.getDiscountId())
                .discountName(entity.getDiscountName())
                .isCash(entity.getIsCash())
                .cash(entity.getCash())
                .percent(entity.getPercent())
                .value(entity.getValue())
                .build();
    }

    // DTO -> Entity
    public BillingDiscount toEntity() {
        return BillingDiscount.builder()
                .discountId(this.discountId)
                .discountName(this.discountName)
                .isCash(this.isCash)
                .cash(this.cash)
                .percent(this.percent)
                .value(this.value)
                .build();
    }
}
