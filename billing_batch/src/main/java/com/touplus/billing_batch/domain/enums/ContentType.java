package com.touplus.billing_batch.domain.enums;

public enum ContentType {
    GROUP,
    YEAR,
    OTHERS;

    public static ContentType from(String value) {
        if (value == null) return null;
        return ContentType.valueOf(value);
    }
}
