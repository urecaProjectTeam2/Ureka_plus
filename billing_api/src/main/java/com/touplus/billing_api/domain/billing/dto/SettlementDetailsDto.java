package com.touplus.billing_api.domain.billing.dto;

import java.util.ArrayList;
import java.util.List;

import com.touplus.billing_api.admin.dto.BillingProductStatResponse;
import com.touplus.billing_api.domain.billing.enums.ProductType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementDetailsDto {


    @Builder.Default
    private List<DetailItem> mobile = new ArrayList<>(); // 휴대폰 요금

    @Builder.Default
    private List<DetailItem> internet = new ArrayList<>(); // 인터넷 요금

    @Builder.Default
    private List<DetailItem> iptv = new ArrayList<>(); // tv

    @Builder.Default
    private List<DetailItem> dps = new ArrayList<>(); // 결합상품

    @Builder.Default
    private List<DetailItem> addon = new ArrayList<>(); // 부가 서비스

    @Builder.Default
    private List<DetailItem> discounts = new ArrayList<>(); // 할인 내역

    @Builder.Default
    private List<DetailItem> unpaids = new ArrayList<>(); // 미납 내역

    @Getter
    @Builder
    public static class DetailItem {
        private String productType;
        private String productName;
        private Integer price;
    }

    public List<BillingProductStatResponse> toProductStatList() {
        List<BillingProductStatResponse> list = new ArrayList<>();

        mobile.forEach(d -> list.add(toProductStat(d)));
        internet.forEach(d -> list.add(toProductStat(d)));
        iptv.forEach(d -> list.add(toProductStat(d)));
        dps.forEach(d -> list.add(toProductStat(d)));
        addon.forEach(d -> list.add(toProductStat(d)));
        discounts.forEach(d -> list.add(toProductStat(d)));
        unpaids.forEach(d -> list.add(toProductStat(d)));

        return list;
    }

    private BillingProductStatResponse toProductStat(DetailItem d) {
        ProductType type = null;
        if (d.getProductType() != null) {
            try {
                type = ProductType.valueOf(d.getProductType().toUpperCase());
            } catch (IllegalArgumentException e) {
                // 변환 실패 시 null 처리, 또는 기본값 설정
                type = null;
            }
        }

        return BillingProductStatResponse.builder()
                .productName(d.getProductName())
                .productType(type)
                .price(d.getPrice())
                .subscribeCount(1L) // 실제 구독자 수 있으면 바꾸기
                .build();
    }

}
