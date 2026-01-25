package com.touplus.billing_batch.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import com.touplus.billing_batch.domain.dto.SettlementDetailsDto.DetailItem;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingWorkDto {
    private BillingUserBillingInfoDto rawData; // DB에서 가져온 원본 데이터
    private int productAmount; // 총 상품 금액
    private int additionalCharges; // 총 추가요금
    private int baseAmount; // 총 상품+추가요금
    private int discountAmount; // 총 할인 금액
    private int unpaidAmount; // 총 미납 금액
    private int totalPrice; // 총 정산 금액
    private int joinedYear;

    // 상세 내역 임시 보관함 (각 프로세서가 여기에 채워넣습니다)
    @Builder.Default
    private List<DetailItem> mobile = new ArrayList<>();
    @Builder.Default
    private List<DetailItem> internet = new ArrayList<>();
    @Builder.Default
    private List<DetailItem> iptv = new ArrayList<>();
    @Builder.Default
    private List<DetailItem> dps = new ArrayList<>();
    @Builder.Default
    private List<DetailItem> addon = new ArrayList<>();
    @Builder.Default
    private List<DetailItem> discounts = new ArrayList<>();
    @Builder.Default
    private List<DetailItem> unpaids = new ArrayList<>();
}
