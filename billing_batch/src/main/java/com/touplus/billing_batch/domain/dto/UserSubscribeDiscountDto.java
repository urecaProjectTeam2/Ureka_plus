package com.touplus.billing_batch.domain.dto;

import com.touplus.billing_batch.domain.entity.BillingDiscount;
import com.touplus.billing_batch.domain.entity.BillingProduct;
import com.touplus.billing_batch.domain.entity.BillingUser;
import com.touplus.billing_batch.domain.entity.UserSubscribeDiscount;
import com.touplus.billing_batch.domain.enums.DiscountType;
import com.touplus.billing_batch.domain.enums.ProductType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
    private LocalDateTime deletedAt;

    public static UserSubscribeDiscountDto fromEntity(UserSubscribeDiscount entity) {
        return UserSubscribeDiscountDto.builder()
                .udsId(entity.getUdsId())
                .discountSubscribeMonth(entity.getDiscountSubscribeMonth())
                .userId(entity.getUserId())
                .discountId(entity.getDiscountId())
                .productId(entity.getProductId())
                .deletedAt(entity.getDeletedAt())
                .build();
    }

    public UserSubscribeDiscount toEntity() {
        return UserSubscribeDiscount.builder()
                .udsId(this.udsId)
                .discountSubscribeMonth(this.discountSubscribeMonth)
                .userId(this.userId)
                .discountId(this.discountId)
                .productId(this.productId)
                .deletedAt((this.deletedAt))
                .build();
    }
}
