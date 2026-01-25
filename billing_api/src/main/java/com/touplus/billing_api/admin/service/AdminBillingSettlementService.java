package com.touplus.billing_api.admin.service;

import java.time.LocalDate;

import com.touplus.billing_api.admin.dto.AdminUserSettlementResponse;
import com.touplus.billing_api.admin.dto.PageResponseDto;

public interface AdminBillingSettlementService {
    PageResponseDto<AdminUserSettlementResponse> getMonthlySettlementResults(LocalDate settlementMonth, int page, int size);
}
