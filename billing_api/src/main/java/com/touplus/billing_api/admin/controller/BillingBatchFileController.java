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
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .filter(file -> {
                String name = file.getFileName();
                // 1. 년도 필터
                boolean yMatch = (year == null || year.isEmpty() || name.contains(year));

                // 2. 월 필터 (년도가 있을 때만 연 조합)
                boolean mMatch = true;
                if (month != null && !month.isEmpty()) {
                    String pattern = (year != null && !year.isEmpty()) ? year + month : month;
                    mMatch = name.contains(pattern);
                }

                // 3. 일 필터 (년/월이 있을 때만 조합)
                boolean dMatch = true;
                if (day != null && !day.isEmpty()) {
                    String pattern = (year != null && !year.isEmpty() && month != null && !month.isEmpty())
                                     ? year + month + day : day;
                    dMatch = name.contains(pattern);
                }

                    return yMatch && mMatch && dMatch;
                })
                .collect(Collectors.toList());

    // ### [수정 핵심] 정렬 로직 추가 ###
    if ("old".equals(sort)) {
        // 오래된 순 (날짜 오름차순)
        logFiles.sort((f1, f2) -> f1.getDateTime().compareTo(f2.getDateTime()));
    } else {
        // 최신순 (날짜 내림차순 - 기본값)
        logFiles.sort((f1, f2) -> f2.getDateTime().compareTo(f1.getDateTime()));
    }

    model.addAttribute("logFiles", logFiles);
    model.addAttribute("currentPath", request.getRequestURI());
    model.addAttribute("selectedYear", year);
    model.addAttribute("selectedMonth", month);
    model.addAttribute("selectedDay", day);
    model.addAttribute("selectedSort", (sort == null || sort.isEmpty()) ? "latest" : sort);

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