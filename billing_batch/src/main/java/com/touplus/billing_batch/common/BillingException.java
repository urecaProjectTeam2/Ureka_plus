package com.touplus.billing_batch.common;

import lombok.Getter;

// 커스텀 예외
@Getter
public class BillingException extends RuntimeException {

    private final String errorCode;
    private final Long userId;

    public BillingException(String message, String errorCode, Long userId) {
        super(message);
        this.errorCode = errorCode;
        this.userId = userId;
    }

    public static BillingException dataNotFound(Long userId, String message) {
        return new BillingException(message, "ERR_DATA_NOT_FOUND", userId);
    }

    public static BillingException invalidAmount(Long userId) {
        return new BillingException("정산 금액이 음수이거나 계산 로직에 오류가 있습니다.", "ERR_INVALID_AMOUNT", userId);
    }

    public static BillingException invalidAdditionalChargeData(Long userId, String acId) {
        return new BillingException("추가 요금에 비정상적인 데이터가 삽입되어 있습니다: " + acId, "ERR_INVALID_ADDITIONAL_CHARGE_DATA", userId);
    }

    public static BillingException invalidProductData(Long userId, String pId) {
        return new BillingException("상품에 비정상적인 데이터가 삽입되어 있습니다: " + pId, "ERR_INVALID_PRODUCT_DATA", userId);
    }

    public static BillingException invalidDiscountData(Long userId, String dId) {
        return new BillingException("할인에 비정상적인 데이터가 삽입되어 있습니다: " + dId, "ERR_INVALID_DISCOUNT_DATA", userId);
    }

    public static BillingException invalidUnpaidAmount(Long userId, String uId) {
        return new BillingException("미납에 비정상적인 데이터가 삽입되어 있습니다: " + uId, "ERR_INVALID_UNPAID_DATA", userId);
    }
}