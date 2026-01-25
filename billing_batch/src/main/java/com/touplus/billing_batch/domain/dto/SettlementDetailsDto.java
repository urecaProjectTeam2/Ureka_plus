package com.touplus.billing_batch.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.touplus.billing_batch.domain.enums.ProductType;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY) // 빈 리스트나 Null인 필드는 JSON 생성 시, 아예 제외하여 데이터 크기를 줄이는 용도.
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

    @Getter
    @Builder
    public static class DetailItem {
        private String productType;
        private String productName;
        private int price;
    }
}
