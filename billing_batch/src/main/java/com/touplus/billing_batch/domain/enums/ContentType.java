package com.touplus.billing_batch.domain.enums;

public enum ContentType {
    group,
    year,
    others;

    public static ContentType from(String value) {
        if (value == null) return null;
        return ContentType.valueOf(value);
    }
}
