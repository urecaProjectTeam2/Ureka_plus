package com.touplus.billing_message.domain.entity;

/**
 * 메시지 상태 Enum
 */
public enum MessageStatus {
    WAITED,   // 메시지 db 저장
    CREATED,  // 생성됨 (템플릿 결합 완료)
    SENT,     // 발송 완료
    FAILED,   // 발송 실패
    BLOCKED   // 발송 차단 (금지 시간대)
}
