package com.touplus.billing_api.admin.service.impl;

import com.touplus.billing_api.admin.service.MessageTemplateService;
import com.touplus.billing_api.domain.message.entity.MessageTemplate;
import com.touplus.billing_api.domain.message.enums.MessageType;
import com.touplus.billing_api.domain.repository.message.MessageTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageTemplateServiceImpl implements MessageTemplateService {

    private final MessageTemplateRepository messageTemplateRepository;

    @Override
    public Long createTemplate(
            String templateName,
            MessageType messageType,
            String templateContent
    ) {
        return messageTemplateRepository.save(
                templateName,
                messageType,
                templateContent
        );
    }

    @Override
    public MessageTemplate getTemplate(Long templateId) {
        return messageTemplateRepository.findById(templateId)
                .orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 메시지 템플릿입니다. id=" + templateId)
                );
    }

    @Override
    public List<MessageTemplate> getTemplates() {
        return messageTemplateRepository.findAll();
    }

    @Override
    public List<MessageTemplate> getTemplatesByType(MessageType messageType) {
        return messageTemplateRepository.findByMessageType(messageType);
    }

    @Override
    public void updateTemplate(
            Long templateId,
            String templateName,
            MessageType messageType,
            String templateContent
    ) {
        // 존재 여부 체크
        getTemplate(templateId);

        messageTemplateRepository.update(
                templateId,
                templateName,
                messageType,
                templateContent
        );
    }

    @Override
    public void deleteTemplate(Long templateId) {
        // 존재 여부 체크
        getTemplate(templateId);

        messageTemplateRepository.delete(templateId);
    }
}
