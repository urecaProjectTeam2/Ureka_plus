package com.touplus.billing_batch.domain.dto;

import com.touplus.billing_batch.domain.entity.UserSubscribeProduct;
import com.touplus.billing_batch.domain.enums.ProductType;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSubscribeProductDto {

    private Long userSubscribeProductId;
    private LocalDate createdMonth;
    private LocalDateTime deletedAt;
    private Long userId;
    private Long productId;

    // Entity -> DTO 변환
    public static UserSubscribeProductDto fromEntity(UserSubscribeProduct entity) {
        if (entity == null) return null;

        return UserSubscribeProductDto.builder()
                .userSubscribeProductId(entity.getUserSubscribeProductId())
                .createdMonth(entity.getCreatedMonth())
                .deletedAt(entity.getDeletedAt())
                .userId(entity.getUserId())
                .productId(entity.getProductId())
                .build();
    }

    // DTO -> Entity 변환
    public UserSubscribeProduct toEntity() {
        return UserSubscribeProduct.builder()
                .userSubscribeProductId(this.userSubscribeProductId)
                .createdMonth(this.createdMonth)
                .deletedAt(this.deletedAt)
                .userId(this.userId)
                .productId(this.productId)
                .build();
    }
}