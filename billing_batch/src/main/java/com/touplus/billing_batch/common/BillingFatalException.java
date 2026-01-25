package com.touplus.billing_batch.common;

import lombok.Getter;

@Getter
public class BillingFatalException extends RuntimeException {

    private final String errorCode;
    private final Long userId;

    public BillingFatalException(String message, String errorCode, Long userId) {
        super(message);
        this.errorCode = errorCode;
        this.userId = userId;
    }

    public static BillingFatalException cacheNotFound(String message) {
        return new BillingFatalException(message, "ERR_CACHE_NOT_FOUND", 0L);
    }

    public static BillingFatalException dataNotFound(String message) {
        return new BillingFatalException(message, "ERR_DATA_NOT_FOUND", 0L);
    }

    public static BillingFatalException invalidProductAmount(Long userId, double productSum, double additionalSum, double baseAmount) {
        return new BillingFatalException(
                "상품 로직 이상 userId=" + userId
                        + " productSum=" + productSum
                        + " additionalSum=" + additionalSum
                        + " baseAmount=" + baseAmount,
                "FATAL_INVALID_AMOUNT",
                userId
        );
    }

    public static BillingFatalException noUsersInPartition(Long minValue, Long maxValue) {
        return new BillingFatalException(
                "Partition 조회 결과 없음: minValue=" + minValue + ", maxValue=" + maxValue,
                "FATAL_NO_USERS_IN_PARTITION",
                0L
        );
    }

    public static BillingFatalException invalidDiscountAmount(Long userId, double totalDiscount) {
        return new BillingFatalException(
                "할인 로직 이상 userId=" + userId
                        + " totalDiscount=" + totalDiscount,
                "FATAL_INVALID_AMOUNT",
                userId
        );
    }
}
