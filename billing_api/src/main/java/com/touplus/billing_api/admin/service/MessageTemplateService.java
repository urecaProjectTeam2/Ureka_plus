package com.touplus.billing_api.admin.service;

import com.touplus.billing_api.domain.message.entity.MessageTemplate;
import com.touplus.billing_api.domain.message.enums.MessageType;

import java.util.List;

public interface MessageTemplateService {

    public Long createTemplate(String templateName, MessageType messageType, String templateContent);

    MessageTemplate getTemplate(Long templateId);

    List<MessageTemplate> getTemplates();

    List<MessageTemplate> getTemplatesByType(MessageType messageType);

    void updateTemplate(
            Long templateId,
            String templateName,
            MessageType messageType,
            String templateContent
    );

    void deleteTemplate(Long templateId);
}
