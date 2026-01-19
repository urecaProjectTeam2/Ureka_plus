package com.touplus.billing_batch.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingWorkDto {
    private BillingUserBillingInfoDto rawData; // DB에서 가져온 원본 데이터
    private int productAmount; // 총 상품 금액
    private int additionalCharges; // 총 추가요금
    private int discountAmount; // 총 할인 금액
    private int unpaidAmount; // 총 미납 금액
    private int totalPrice; // 총 정산 금액
}
