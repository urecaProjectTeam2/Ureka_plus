package com.touplus.billing_api.domain.repository.message;

import com.touplus.billing_api.domain.message.entity.MessageTemplate;
import com.touplus.billing_api.domain.message.enums.MessageType;

import java.util.List;
import java.util.Optional;

public interface MessageTemplateRepository {

    // Create
    Long save(String templateName, MessageType messageType, String templateContent);

    // Read
    Optional<MessageTemplate> findById(Long templateId);
    List<MessageTemplate> findAll();                     // 전체 조회
    List<MessageTemplate> findByMessageType(MessageType messageType);

    // Update
    void update(Long templateId, String templateName, MessageType messageType, String templateContent);

    // Delete (soft delete)
    void delete(Long templateId);
}