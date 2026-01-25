package com.touplus.billing_api.domain.message.dto;

import lombok.Builder;

@Builder
public record UserContactDto(
        Long userId,
        String name,
        String email,
        String phone
) {
}
