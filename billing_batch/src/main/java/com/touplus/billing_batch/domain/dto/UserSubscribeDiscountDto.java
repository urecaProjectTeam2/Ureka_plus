package com.touplus.billing_batch.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSubscribeDiscountDto {
    private Long udsId;
    private LocalDate discountSubscribeMonth;
    private Long userId;
    private Long discountId;
    private Long productId;
}
