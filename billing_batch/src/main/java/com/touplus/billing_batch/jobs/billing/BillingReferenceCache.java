package com.touplus.billing_batch.jobs.billing;

import com.touplus.billing_batch.domain.dto.BillingDiscountDto;
import com.touplus.billing_batch.domain.dto.BillingProductDto;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Getter
@Component
public class BillingReferenceCache {
    // volatile을 사용하여 모든 스레드에 즉시 변경사항이 보이도록 보장
    private volatile Map<Long, BillingProductDto> productMap = new HashMap<>();
    private volatile Map<Long, BillingDiscountDto> discountMap = new HashMap<>();

    public void updateProducts(Map<Long, BillingProductDto> newMap) {
        this.productMap = Collections.unmodifiableMap(newMap); // 읽기 전용으로 안전하게 교체
    }

    public void updateDiscounts(Map<Long, BillingDiscountDto> newMap) {
        this.discountMap = Collections.unmodifiableMap(newMap);
    }

    public Map<Long, BillingProductDto> getProductMap() {
        return productMap;
    }

    public Map<Long, BillingDiscountDto> getDiscountMap() {
        return discountMap;
    }

    public void clear() {
        productMap = new HashMap<>();
        discountMap = new HashMap<>();
    }
}