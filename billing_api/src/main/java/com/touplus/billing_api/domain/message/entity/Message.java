package com.touplus.billing_api.domain.message.entity;

import com.touplus.billing_api.domain.message.enums.MessageStatus;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * message 테이블 Row 매핑용 객체 (JDBC)
 */
@Getter
@AllArgsConstructor
public class Message {

    @Id
    @Column(name = "message_id")
    private final Long messageId;

    @Column(name = "billing_id")
    private final Long billingId;

    @Column(name = "user_id")
    private final Long userId;

    @Column(name = "status")
    private final MessageStatus status;

    @Column(name = "scheduled_at")
    private final LocalDateTime scheduledAt;

    @Column(name = "retry_count")
    private final Integer retryCount;

    @Column(name = "ban_end_time")
    private final LocalTime banEndTime;


}
