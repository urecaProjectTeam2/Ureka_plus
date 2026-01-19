package com.touplus.billing_message.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * message 테이블 Entity
 * 발송 스케줄 관리용
 */
@Entity
@Table(name = "message",
    indexes = {
        @Index(name = "idx_message_status_schedule", columnList = "status, scheduled_at")
    }
)
@Getter
@NoArgsConstructor
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // AUTO_INCREMENT
    @Column(name = "message_id")
    private Long messageId;

    @Column(name = "billing_id", nullable = false)
    private Long billingId;  // snapshot에서 복사

    @Column(name = "user_id", nullable = false)
    private Long userId;     // snapshot에서 복사

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MessageStatus status;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;  // 발송 예정 시간

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    /**
     * Message 생성자
     */
    public Message(Long billingId, Long userId, LocalDateTime scheduledAt) {
        this.billingId = billingId;
        this.userId = userId;
        this.status = MessageStatus.WAITED;  // 고정값
        this.scheduledAt = scheduledAt;
        this.retryCount = 0;                 // 고정값
    }
}
