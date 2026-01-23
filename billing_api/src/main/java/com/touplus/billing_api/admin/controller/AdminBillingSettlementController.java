package com.touplus.billing_api.admin.controller;

import com.touplus.billing_api.admin.dto.AdminUserSettlementResponse;
import com.touplus.billing_api.admin.dto.PageResponse;
import com.touplus.billing_api.admin.service.AdminBillingSettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin/settlements")
@RequiredArgsConstructor
public class AdminBillingSettlementController {

    private final AdminBillingSettlementService adminBillingSettlementService;

    /**
     * 관리자용 월별 정산 결과 조회
     * - 정산이 없는 유저도 포함
     */
    @GetMapping
    public PageResponse<AdminUserSettlementResponse> getMonthlySettlements(
            @RequestParam
            @DateTimeFormat(pattern = "yyyy-MM") LocalDate settlementMonth,

            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return adminBillingSettlementService.getMonthlySettlementResults(
                settlementMonth,
                page,
                size
        );
    }
}
