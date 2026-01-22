package com.touplus.billing_api.common.masking;


public class MaskingUtils {

    // 010-1234-5678 → 010-****-5678
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.replaceAll("(\\d{3})-?(\\d{3,4})-?(\\d{4})", "$1-****-$3");
    }

    // test@email.com → te**@email.com
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }

        String[] parts = email.split("@");
        String local = parts[0];
        String domain = parts[1];

        if (local.length() <= 2) {
            return "*@"+domain;
        }

        return local.substring(0, 2)
                + "*".repeat(local.length() - 2)
                + "@"
                + domain;
    }

    // 김영희 → 김**
    public static String maskName(String name) {
        if (name == null || name.length() < 2) {
            return name;
        }
        return name.charAt(0) + "*".repeat(name.length() - 1);
    }
}