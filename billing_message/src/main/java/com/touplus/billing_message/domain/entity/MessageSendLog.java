package com.touplus.billing_message.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "message_send_log")
@Getter
@NoArgsConstructor
public class MessageSendLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_send_log_id")
    private Long id;

    @Column(name = "message_id", nullable = false)
    private Long messageId;

    @Column(name = "retry_no", nullable = false)
    private int retryNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    private MessageType messageType;

    @Column(name = "provider_response_code", nullable = false)
    private String providerResponseCode;

    @Column(name = "provider_response_message")
    private String providerResponseMessage;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    public MessageSendLog(
            Long messageId,
            int retryNo,
            MessageType messageType,
            String providerResponseCode,
            String providerResponseMessage,
            LocalDateTime sentAt
    ) {
        this.messageId = messageId;
        this.retryNo = retryNo;
        this.messageType = messageType;
        this.providerResponseCode = providerResponseCode;
        this.providerResponseMessage = providerResponseMessage;
        this.sentAt = sentAt;
    }
}
