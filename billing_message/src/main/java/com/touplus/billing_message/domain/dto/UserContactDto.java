package com.touplus.billing_message.domain.dto;

import lombok.Builder;

@Builder
public record UserContactDto(
        Long userId,
        String name,
        String email,
        String phone
) {
}