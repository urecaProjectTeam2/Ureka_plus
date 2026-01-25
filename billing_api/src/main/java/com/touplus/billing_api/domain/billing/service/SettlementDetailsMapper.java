package com.touplus.billing_api.domain.billing.service;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.touplus.billing_api.domain.billing.dto.SettlementDetailsDto;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SettlementDetailsMapper {

    private final ObjectMapper objectMapper;

    // 단일 SettlementDetailsDto 반환
    public SettlementDetailsDto fromJson(String json) {
        if (json == null || json.isBlank()) {
            return new SettlementDetailsDto(); // 빈 DTO 반환
        }

        try {
            return objectMapper.readValue(json, SettlementDetailsDto.class);
        } catch (Exception e) {
            throw new IllegalStateException("정산 상세 JSON 파싱 실패", e);
        }
    }
}
