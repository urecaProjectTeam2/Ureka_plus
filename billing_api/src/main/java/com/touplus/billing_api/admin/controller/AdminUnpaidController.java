package com.touplus.billing_api.admin.controller;

import com.touplus.billing_api.admin.dto.PageResponseDto;
import com.touplus.billing_api.admin.dto.UnpaidUserResponse;
import com.touplus.billing_api.admin.service.AdminUnpaidService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/unpaid")
@RequiredArgsConstructor
public class AdminUnpaidController {

    private final AdminUnpaidService adminUnpaidService;

    @GetMapping
    public PageResponseDto<UnpaidUserResponse> getUnpaidUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return adminUnpaidService.getUnpaidUsers(page, size);
    }

    @GetMapping("/search")
    public PageResponseDto<UnpaidUserResponse> searchUnpaidUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword
    ) {
        return adminUnpaidService.searchUnpaidUsersByKeyword(page, size, keyword);
    }
}
