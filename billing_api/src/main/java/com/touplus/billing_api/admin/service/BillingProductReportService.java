package com.touplus.billing_api.admin.service;

import com.touplus.billing_api.admin.dto.BillingProductStatResponse;

import java.util.List;

public interface BillingProductReportService {
    /**
     * 특정 타입 상품 기준으로 구독 많은 순 Top N 조회
     * @param productTypes 상품 타입 리스트 (예: "movie", "addon")
     * @param limit 조회 수
     * @return 구독자 수 포함 상품 DTO 리스트
     */
    List<BillingProductStatResponse> getTopSubscribedProducts(List<String> productTypes, int limit);
}
