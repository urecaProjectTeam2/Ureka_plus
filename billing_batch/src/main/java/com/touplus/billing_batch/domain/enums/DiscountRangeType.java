package com.touplus.billing_batch.domain.enums;

public enum DiscountRangeType {
    MOBILE,
    INTERNET,
    IPTV,
    DPS,
    MOBILE_INTERNET;

    public static DiscountRangeType from(String value) {
        if (value == null) return null;
        return DiscountRangeType.valueOf(value);
    }
}
