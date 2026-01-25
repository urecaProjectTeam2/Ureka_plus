package com.touplus.billing_api.admin.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductDto {
    private String productName;
    private int subscribeCount;
}
