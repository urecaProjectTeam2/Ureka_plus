package com.touplus.billing_api.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin/logs")
@RequiredArgsConstructor
public class BillingBatchFileController {

    private final BillingLogService billingLogService;

    /**
     * 1. 로그 파일 목록 조회 페이지
     */
    @GetMapping
    public String listLogs(Model model) {
        List<String> logFiles = billingLogService.getLogFileList();
        model.addAttribute("logFiles", logFiles);
        return "admin/log_list"; // resources/templates/admin/log_list.html 필요
    }

    /**
     * 2. 특정 로그 파일 다운로드
     */
    @GetMapping("/download/{fileName}")
    public ResponseEntity<Resource> downloadLog(@PathVariable String fileName) {
        return billingLogService.downloadLogFile(fileName);
    }
}