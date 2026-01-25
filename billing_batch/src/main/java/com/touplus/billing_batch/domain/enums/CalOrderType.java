package com.touplus.billing_batch.domain.enums;

public enum CalOrderType {
    SINGLE,
    MULTI,
    ALL;

    public static CalOrderType from(String value) {
        if (value == null) return null;
        return CalOrderType.valueOf(value);
    }
}
