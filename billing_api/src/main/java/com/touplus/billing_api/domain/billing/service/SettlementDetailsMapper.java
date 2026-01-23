package com.touplus.billing_api.domain.billing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.touplus.billing_api.domain.billing.dto.SettlementDetailsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SettlementDetailsMapper {

    private final ObjectMapper objectMapper;

    public SettlementDetailsDto fromJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readValue(json, SettlementDetailsDto.class);
        } catch (Exception e) {
            throw new IllegalStateException("정산 상세 JSON 파싱 실패", e);
        }
    }
}