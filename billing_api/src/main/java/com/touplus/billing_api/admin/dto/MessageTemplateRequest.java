package com.touplus.billing_api.admin.dto;


import com.touplus.billing_api.domain.message.enums.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class MessageTemplateRequest {

    @NotBlank
    private String templateName;

    @NotNull
    private MessageType messageType;

    @NotBlank
    private String templateContent;
}
