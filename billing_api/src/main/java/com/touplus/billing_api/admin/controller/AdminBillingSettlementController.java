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
            @RequestParam(value = "settlementMonth", required = false) String settlementMonthStr,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            Model model) {

        LocalDate settlementMonth;
        if (settlementMonthStr == null || settlementMonthStr.isEmpty()) {
            settlementMonth = LocalDate.of(2025, 12, 1);
        } else {
            settlementMonth = LocalDate.parse(settlementMonthStr + "-01"); // "yyyy-MM-01"로 변환
        }

        var pageResponse = adminBillingSettlementService.getMonthlySettlementResults(settlementMonth, page, size);

        model.addAttribute("settlements", pageResponse);
        model.addAttribute("currentPage", page);

        // String으로 HTML에서 쓰기 위한 값
        model.addAttribute("settlementMonthStr", settlementMonth.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")));
        model.addAttribute("currentPath", "/admin/users/settlements");

        return "billing-result";
    }

}
