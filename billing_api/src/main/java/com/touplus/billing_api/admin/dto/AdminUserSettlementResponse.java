package com.touplus.billing_api.admin.dto;

import java.time.LocalDate;
import java.util.List;

import com.touplus.billing_api.domain.message.dto.UserContactDto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminUserSettlementResponse {

    private Long billingResultId;          // 정산 결과 ID
    private LocalDate settlementMonth;     // 정산 월

    private UserContactDto user;            // 사용자 정보 (복호화 + 마스킹)
    private Integer totalPrice;             // 총 정산 금액

    private List<BillingProductStatResponse> details;   // 정산 상세(JSON 파싱)
}
