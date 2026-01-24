package com.touplus.billing_batch.domain.enums;

public enum CalcOrderType {
    SINGLE,
    MULTI,
    ALL;

    public static CalcOrderType from(String value) {
        if (value == null) return null;
        return CalcOrderType.valueOf(value);
    }
}
