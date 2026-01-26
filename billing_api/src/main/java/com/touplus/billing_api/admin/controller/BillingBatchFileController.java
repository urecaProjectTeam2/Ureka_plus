package com.touplus.billing_api.admin.controller;

import com.touplus.billing_api.admin.dto.BillingLogFile;
import com.touplus.billing_api.admin.service.BillingLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.Objects;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/logs")
@RequiredArgsConstructor
public class BillingBatchFileController {

    private final BillingLogService billingLogService;

    /**
     * 로그 파일 목록 조회 (필터 및 정렬 포함)
     */
    @GetMapping
    public String listLogs(
    		@RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "year", required = false) String year,
            @RequestParam(value = "month", required = false) String month,
            @RequestParam(value = "day", required = false) String day,
            HttpServletRequest request, Model model) {

        List<String> rawFileNames = billingLogService.getLogFileList();
        System.out.println("DEBUG: 서비스에서 가져온 파일명 리스트 = " + rawFileNames);

        List<BillingLogFile> logFiles = rawFileNames.stream()
                .map(name -> {
                    try {
                        return new BillingLogFile(name, billingLogService.checkErrorInFile(name));
                    } catch (Exception e) {
                        System.out.println("DEBUG: 객체 변환 중 에러 발생 파일명 = " + name + " | 에러: " + e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull) // 에러난 객체 제외
                .filter(file -> {
                    String name = file.getFileName();
                    // 필터 조건이 null이거나 빈 문자열이면 무조건 pass(true)
                    boolean yMatch = (year == null || year.isEmpty() || name.contains(year));
                    boolean mMatch = (month == null || month.isEmpty() || name.contains(year + month));
                    boolean dMatch = (day == null || day.isEmpty() || name.contains(year + month + day));

                    return yMatch && mMatch && dMatch;
                })
                .collect(Collectors.toList());

        System.out.println("DEBUG: 필터링 후 남은 객체 개수 = " + logFiles.size());

        model.addAttribute("logFiles", logFiles);
        model.addAttribute("currentPath", request.getRequestURI());
        model.addAttribute("selectedYear", year);
        model.addAttribute("selectedMonth", month);
        model.addAttribute("selectedDay", day);
        model.addAttribute("selectedSort", sort);

        return "admin/log_list";
    }

    @GetMapping("/view/{fileName}")
    public String viewLog(@PathVariable("fileName") String fileName, Model model) {
        model.addAttribute("fileName", fileName);
        model.addAttribute("logLines", billingLogService.readLogFile(fileName));
        model.addAttribute("currentPath", "/admin/logs");
        return "admin/log_view";
    }

    @GetMapping("/download/{fileName}")
    public ResponseEntity<Resource> downloadLog(@PathVariable("fileName") String fileName) {
        return billingLogService.downloadLogFile(fileName);
    }
}