package com.touplus.billing_api.admin.service;

import com.touplus.billing_api.admin.dto.AdminUserSettlementResponse;
import com.touplus.billing_api.admin.dto.PageResponse;

import java.time.LocalDate;

public interface AdminBillingSettlementService {
    PageResponse<AdminUserSettlementResponse> getMonthlySettlementResults(LocalDate settlementMonth, int page, int size);
}
