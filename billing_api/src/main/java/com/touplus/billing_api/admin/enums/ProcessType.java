package com.touplus.billing_api.admin.enums;

/* 영어 버전
public enum ProcessType {
	WAITED,
	PENDING,
	DONE,
	FAIL
}*/

// 한국어 버전
public enum ProcessType {
    WAITED("대기"),
    PENDING("진행 중"),
    DONE("완료"),
    FAIL("실패");

    private final String label;

    ProcessType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
