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
    public static BillingUserDto fromEntity(com.touplus.billing_batch.domain.entity.BillingUser entity) {
        return BillingUserDto.builder()
                .userId(entity.getUserId())
                .build();
    }

    // DTO -> Entity 변환
    public BillingUser toEntity() {
        BillingUser user = new com.touplus.billing_batch.domain.entity.BillingUser();
        // ID는 JPA에서 자동 생성되므로, 신규 생성 시는 set 하지 않아도 됨
        return user;
    }
}
