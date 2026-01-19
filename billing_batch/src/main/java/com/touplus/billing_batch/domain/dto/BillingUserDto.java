package com.touplus.billing_batch.domain.dto;

import com.touplus.billing_batch.domain.entity.BillingUser;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingUserDto {

    private Long userId;

    // Entity -> DTO 변환
    public static BillingUserDto fromEntity(BillingUser entity) {
        if (entity == null) return null;
        return BillingUserDto.builder()
                .userId(entity.getUserId())
                .build();
    }

    // DTO -> Entity 변환
    public BillingUser toEntity() {
        return BillingUser.builder()
                .userId(this.userId)
                .build();
    }
}
