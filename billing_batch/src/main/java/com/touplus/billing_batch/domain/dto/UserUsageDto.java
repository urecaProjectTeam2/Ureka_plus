package com.touplus.billing_batch.domain.dto;

import com.touplus.billing_batch.domain.entity.UserUsage;
import com.touplus.billing_batch.domain.enums.UsageType;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUsageDto {

    private Long userUsageId;
    private Long userId;
    private LocalDate useMonth;
    private UsageType useType;
    private Integer useAmount;

    // Entity -> DTO
    public static UserUsageDto fromEntity(UserUsage entity) {
        if (entity == null) return null;

        return UserUsageDto.builder()
                .userUsageId(entity.getUserUsageId())
                .userId(entity.getUserId())
                .useMonth(entity.getUseMonth())
                .useType(entity.getUseType())
                .useAmount(entity.getUseAmount())
                .build();
    }

    // DTO -> Entity
    public UserUsage toEntity() {
        return UserUsage.builder()
                .userUsageId(this.userUsageId)
                .userId(this.userId)
                .useMonth(this.useMonth)
                .useType(this.useType)
                .useAmount(this.useAmount)
                .build();
    }
}