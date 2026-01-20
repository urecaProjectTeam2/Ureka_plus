package com.touplus.billing_batch.common;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.touplus.billing_batch.domain.dto.BillingDiscountDto;
import com.touplus.billing_batch.domain.dto.BillingProductDto;
import lombok.Getter;
import org.springframework.stereotype.Component;

@Getter
@Component
public class BillingReferenceCache {

    private final Map<Long, BillingProductDto> productMap = new ConcurrentHashMap<>();
    private final Map<Long, BillingDiscountDto> discountMap = new ConcurrentHashMap<>();

    public void clear() {
        productMap.clear();
        discountMap.clear();
    }
}
