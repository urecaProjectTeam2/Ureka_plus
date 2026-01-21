package com.touplus.billing_batch.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.touplus.billing_batch.domain.enums.ProductType;
import lombok.*;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY) // 빈 리스트나 Null인 필드는 JSON 생성 시, 아예 제외하여 데이터 크기를 줄이는 용도.
public class SettlementDetailsDto {

    private List<DetailItem> mobile;      // 휴대폰 요금
    private List<DetailItem> internet;      // 인터넷 요금
    private List<DetailItem> iptv;      // tv
    private List<DetailItem> dps;      // 결합상품
    private List<DetailItem> addon;     // 부가 서비스
    private List<DetailItem> discounts; // 할인 내역
    private List<DetailItem> unpaids;   // 미납 내역

    @Getter
    @Builder
    public static class DetailItem {
        private String productType;
        private String productName;
        private Integer price;
    }
}
