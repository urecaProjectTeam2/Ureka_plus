package com.touplus.billing_api.admin.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.touplus.billing_api.admin.service.AdminBillingSettlementService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/users/settlements")
@RequiredArgsConstructor
public class AdminBillingSettlementController {

    private final AdminBillingSettlementService adminBillingSettlementService;

    @GetMapping
    public String dashboard(
            @RequestParam(value = "settlementMonth", required = false)
            @DateTimeFormat(pattern = "yyyy-MM") LocalDate settlementMonth,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            Model model, 
            HttpServletRequest request
    ) {
        // settlementMonth가 null이면 이번 달 1일로 설정
        if (settlementMonth == null) {
            settlementMonth = LocalDate.now().withDayOfMonth(1);
        }

        // 서비스 호출
        var pageResponse = adminBillingSettlementService.getMonthlySettlementResults(settlementMonth, page, size);

        // 모델 세팅
        model.addAttribute("settlements", pageResponse);   // null-safe 템플릿 기준
        model.addAttribute("currentPage", page);
        model.addAttribute("settlementMonth", settlementMonth);

        return "billing-result"; // templates/billing-result.html
    }
}
