package com.touplus.billing_api.admin.controller;

import com.touplus.billing_api.admin.dto.MessageTemplateRequest;
import com.touplus.billing_api.admin.service.MessageTemplateService;
import com.touplus.billing_api.domain.message.entity.MessageTemplate;
import com.touplus.billing_api.domain.message.enums.MessageType;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/process")
public class AdminMessageTemplateController {

    private final MessageTemplateService messageTemplateService;

    // 타임리프
    @GetMapping("/templates/management")
    public String templateManagementPage(
            @RequestParam(value = "messageType", required = false) String messageType,
            Model model,
            HttpServletRequest request
    ) {
        List<MessageTemplate> templates;

        if (messageType == null || messageType.isBlank()) {
            templates = messageTemplateService.getTemplates(); // 전체
        } else {
            templates = messageTemplateService.getTemplatesByType(
                    MessageType.valueOf(messageType)
            );
        }

        model.addAttribute("templates", templates);
        model.addAttribute("messageTypes", MessageType.values());
        model.addAttribute("selectedType", messageType); // 선택 유지용 (옵션)
        model.addAttribute("currentPath", request.getRequestURI());
        
        return "template-management";
    }


    // crud
    @PostMapping("/templates")
    @ResponseBody
    public Long createTemplateApi(@RequestBody @Valid MessageTemplateRequest request) {
        return messageTemplateService.createTemplate(
                request.getTemplateName(),
                request.getMessageType(),
                request.getTemplateContent()
        );
    }

    @PutMapping("/templates/{templateId}")
    @ResponseBody
    public void updateTemplateApi(
            @PathVariable("templateId") Long templateId,
            @RequestBody @Valid MessageTemplateRequest request
    ) {
        messageTemplateService.updateTemplate(
                templateId,
                request.getTemplateName(),
                request.getMessageType(),
                request.getTemplateContent()
        );
    }

    @DeleteMapping("/templates/{templateId}")
    @ResponseBody
    public void deleteTemplateApi(@PathVariable("templateId") Long templateId) {
        messageTemplateService.deleteTemplate(templateId);
    }

    @GetMapping("/templates/{templateId}")
    @ResponseBody
    public MessageTemplate getTemplateApi(@PathVariable("templateId") Long templateId) {
        return messageTemplateService.getTemplate(templateId);
    }
}
