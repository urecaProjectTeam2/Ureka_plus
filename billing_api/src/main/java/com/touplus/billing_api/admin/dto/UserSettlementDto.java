package com.touplus.billing_api.admin.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserSettlementDto {

    private UserDto user;                   // 유저 정보
    private List<BillingProductStatResponse> products; // 구독 상품 목록 (null 가능)
    private Integer totalPrice;             // 정산 금액
}
