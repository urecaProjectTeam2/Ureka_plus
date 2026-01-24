package com.touplus.billing_batch.domain.dto;

import com.touplus.billing_batch.domain.entity.GroupDiscount;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupDiscountDto {

    private Long groupId;
    private Integer numOfMember;

    // Entity -> DTO 변환
    public static GroupDiscountDto fromEntity(GroupDiscount entity) {
        if (entity == null) return null;

        return GroupDiscountDto.builder()
                .groupId(entity.getGroupId())
                .numOfMember(entity.getNumOfMember())
                .build();
    }

    // DTO -> Entity 변환
    public GroupDiscount toEntity() {
        return GroupDiscount.builder()
                .groupId(this.groupId)
                .numOfMember(this.numOfMember)
                .build();
    }
}
