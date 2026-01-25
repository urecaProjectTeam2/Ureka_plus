package com.touplus.billing_api.admin.dto;

import com.touplus.billing_api.domain.billing.enums.ProductType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class DonutChartView {
    private ProductType productType;
    private String title;
    private List<String> labels;
    private List<Long> data;
}
