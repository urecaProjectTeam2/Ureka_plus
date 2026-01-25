package com.touplus.billing_batch.domain.dto;

import com.touplus.billing_batch.domain.entity.BillingUser;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingUserMemberDto {
    private Long userId;
    private Long groupId;
    private Integer userNumOfMember;
    private Integer groupNumOfMember;
}
