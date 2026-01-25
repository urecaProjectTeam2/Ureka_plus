package com.touplus.billing_batch.domain.enums;

public enum JobType {
    WAITED,
    PENDING,
    DONE,
    FAIL;

    public static JobType from(String value) {
        if (value == null) return null;
        return JobType.valueOf(value);
    }
}