package com.touplus.billing_batch.domain.dto;

import com.touplus.billing_batch.domain.enums.ProductType;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
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
}
