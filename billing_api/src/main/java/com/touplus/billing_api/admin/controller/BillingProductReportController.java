package com.touplus.billing_api.admin.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.touplus.billing_api.admin.dto.BillingProductStatResponse;
import com.touplus.billing_api.admin.service.BillingProductReportService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/users/products")
@RequiredArgsConstructor
public class BillingProductReportController {

    private final BillingProductReportService reportService;

    @GetMapping("/bar")
    public String billingReportBar(Model model, HttpServletRequest request) {

        List<String> productTypes = List.of("MOVIE", "ADDON");

        List<BillingProductStatResponse> topProducts =
                reportService.getTopSubscribedProducts(productTypes, 7);

        model.addAttribute("barLabels",
                topProducts.stream()
                        .map(BillingProductStatResponse::getProductName)
                        .collect(Collectors.toList()));

        model.addAttribute("barData",
                topProducts.stream()
                        .map(BillingProductStatResponse::getSubscribeCount)
                        .collect(Collectors.toList()));

        model.addAttribute("currentPath", request.getRequestURI());
        
        return "productBar";
    }
    
    @GetMapping("/donut")
    public String billingReportDonut(Model model, HttpServletRequest request) {

        List<String> productTypes = List.of("MOVIE", "ADDON");
        
        List<BillingProductStatResponse> allProducts =
                reportService.getTopSubscribedProducts(productTypes, 100);


        model.addAttribute("donutLabels",
                allProducts.stream()
                        .map(BillingProductStatResponse::getProductName)
                        .collect(Collectors.toList()));

        model.addAttribute("donutData",
                allProducts.stream()
                        .map(BillingProductStatResponse::getSubscribeCount)
                        .collect(Collectors.toList()));

        model.addAttribute("currentPath", request.getRequestURI());
        
        return "productDonut";
    }
}
