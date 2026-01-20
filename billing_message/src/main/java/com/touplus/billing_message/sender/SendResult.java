package com.touplus.billing_message.sender;

public record SendResult(boolean success, String code, String message) {
    public static SendResult ok(String code, String message) {
        return new SendResult(true, code, message);
    }

    public static SendResult fail(String code, String message) {
        return new SendResult(false, code, message);
    }
}
