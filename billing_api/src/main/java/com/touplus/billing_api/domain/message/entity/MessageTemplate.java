package com.touplus.billing_api.domain.message.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.touplus.billing_api.domain.message.enums.MessageType;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MessageTemplate {

    @Id
    @Column(name = "template_id")
    @JsonProperty("id")  // JSON 직렬화 시 templateId → id로 변환
    private Long templateId;

    @Column(name = "template_name", nullable = false, length = 30)
    private String templateName;

    @Column(name = "message_type", nullable = false)
    private MessageType messageType;  // SMS or EMAIL

    @Column(name = "template_content", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String templateContent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public MessageTemplate(String templateName, MessageType messageType, String templateContent) {
        this.templateName = templateName;
        this.messageType = messageType;
        this.templateContent = templateContent;
    }
}
