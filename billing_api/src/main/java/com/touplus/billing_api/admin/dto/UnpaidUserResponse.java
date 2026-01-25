package com.touplus.billing_api.admin.dto;

import com.touplus.billing_api.domain.message.dto.UserContactDto;

import java.time.LocalDate;

public record UnpaidUserResponse(
        Long unpaidId,
        Integer unpaidPrice,
        LocalDate unpaidMonth,
        UserContactDto user
) {
}
