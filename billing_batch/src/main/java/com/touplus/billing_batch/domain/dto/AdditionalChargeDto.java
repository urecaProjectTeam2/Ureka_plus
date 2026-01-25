package com.touplus.billing_batch.domain.dto;

import com.touplus.billing_batch.domain.entity.AdditionalCharge;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdditionalChargeDto {

    private Long id;
    private String companyName;
    private Integer price;
    private LocalDate additionalChargeMonth;
    private Long userId;

    // Entity -> DTO 변환
    public static AdditionalChargeDto fromEntity(AdditionalCharge entity) {
        if (entity == null) return null;

        return AdditionalChargeDto.builder()
                .id(entity.getId())
                .companyName(entity.getCompanyName())
                .price(entity.getPrice())
                .additionalChargeMonth(entity.getAdditionalChargeMonth())
                .userId(entity.getUserId())
                .build();
    }

    // DTO -> Entity 변환
    public AdditionalCharge toEntity() {
        return AdditionalCharge.builder()
                .id(this.id)
                .companyName(this.companyName)
                .price(this.price)
                .additionalChargeMonth(this.additionalChargeMonth)
                .userId(this.userId)
                .build();
    }
}