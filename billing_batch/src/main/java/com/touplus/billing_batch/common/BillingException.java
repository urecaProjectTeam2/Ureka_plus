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

    public static BillingException dataNotFound(Long userId) {
        return new BillingException("정산에 필요한 기초 데이터가 존재하지 않습니다.", "ERR_DATA_NOT_FOUND", userId);
    }

    public static BillingException invalidAmount(Long userId) {
        return new BillingException("정산 금액이 음수이거나 계산 로직에 오류가 있습니다.", "ERR_INVALID_AMOUNT", userId);
    }
}