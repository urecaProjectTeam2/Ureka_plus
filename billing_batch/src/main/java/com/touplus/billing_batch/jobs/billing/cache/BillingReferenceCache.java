package com.touplus.billing_batch.jobs.billing.cache;

import com.touplus.billing_batch.domain.dto.*;
import com.touplus.billing_batch.domain.enums.UseType;
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
    private volatile Map<UsageKeyDto, ProductBaseUsageDto> productBaseUsageMap = new HashMap<>();
    private volatile Map<Long, RefundPolicyDto> refundPolicyMap = new HashMap<>();
    private volatile Map<UseType, OverusePolicyDto> overusePolicyMap = new HashMap<>();
    private volatile Map<Long, DiscountPolicyDto> discountPolicyMap = new HashMap<>();

    public void updateProducts(Map<Long, BillingProductDto> newMap) {
        this.productMap = Collections.unmodifiableMap(newMap); // 읽기 전용으로 안전하게 교체
    }

    public void updateDiscounts(Map<Long, BillingDiscountDto> newMap) {
        this.discountMap = Collections.unmodifiableMap(newMap);
    }

    public void updateProductBaseUsageMaps(Map<UsageKeyDto, ProductBaseUsageDto> newMap) {
        this.productBaseUsageMap = Collections.unmodifiableMap(newMap);
    }

    public void updateRefundPolicyMaps(Map<Long, RefundPolicyDto> newMap) {
        this.refundPolicyMap = Collections.unmodifiableMap(newMap);
    }

    public void updateOverusePolicyMaps(Map<UseType, OverusePolicyDto> newMap) {
        this.overusePolicyMap = Collections.unmodifiableMap(newMap);
    }

    public void updateDiscountPolicyMap(Map<Long, DiscountPolicyDto> newMap) {
        this.discountPolicyMap = Collections.unmodifiableMap(newMap);
    }

    public Map<Long, BillingProductDto> getProductMap() {
        return productMap;
    }
    public Map<Long, BillingDiscountDto> getDiscountMap() {
        return discountMap;
    }
    public Map<UsageKeyDto, ProductBaseUsageDto> getProductBaseUsageMap() {
        return productBaseUsageMap;
    }
    public Map<Long, RefundPolicyDto> getRefundPolicyMap() {
        return refundPolicyMap;
    }
    public Map<UseType, OverusePolicyDto> getOverusePolicyMap() {
        return overusePolicyMap;
    }
    public Map<Long, DiscountPolicyDto> getDiscountPolicyMap() {
        return discountPolicyMap;
    }

    public void clear() {
        productMap = Collections.emptyMap();
        discountMap = Collections.emptyMap();
        productBaseUsageMap = Collections.emptyMap();
        refundPolicyMap = Collections.emptyMap();
        overusePolicyMap = Collections.emptyMap();
        discountPolicyMap = Collections.emptyMap();
    }
}