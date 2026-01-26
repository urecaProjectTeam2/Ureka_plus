package com.touplus.billing_api.admin.controller;

import com.touplus.billing_api.admin.dto.BatchBillingErrorLogDto;
import com.touplus.billing_api.admin.repository.JdbcBatchRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/batch")
public class BatchDashBoardController {

    private final JdbcBatchRepository repository;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // 1. 과거 이력 30건을 조회하여 'historyList'라는 이름으로 모델에 담습니다.
        model.addAttribute("historyList", repository.findAllHistory());
        model.addAttribute("lastSuccess", repository.findLatestSuccessfulBatch());
        // 2. src/main/resources/templates/batch-dashboard.html 파일을 찾아갑니다.
        return "batch-dashboard";
    }

    @GetMapping("/dashboard/history-fragment")
    public String getHistoryFragment(Model model) {
        // 기존에 사용하던 이력 조회 메서드 그대로 사용
        model.addAttribute("historyList", repository.findAllHistory());

        // "파일명 :: 프래그먼트이름" 형식으로 리턴
        return "batch-dashboard :: history-table-content";
    }

    @GetMapping("/error/list/{jobExecutionId}")
    public String errorLogList(@PathVariable Long jobExecutionId, Model model) {
        // 해당 잡의 모든 에러 로그 조회 (batch_billing_error_log)
        model.addAttribute("errors", repository.findAllErrorLogsByJobId(jobExecutionId));
        model.addAttribute("jobId", jobExecutionId);
        return "batch-error-log-list"; // 새로 만들 에러 로그 상세 페이지
    }

    @GetMapping("/error/search")
    public String searchErrorLogs(
            @RequestParam Long jobId,
            @RequestParam Long userId,
            Model model) {

        // 1. 리포지토리를 통해 필터링된 에러 로그 조회
        List<BatchBillingErrorLogDto> filteredErrors = repository.findErrorLogsByJobIdAndUserId(jobId, userId);

        // 2. 결과 데이터를 모델에 담아 기존 상세 페이지로 반환
        model.addAttribute("errors", filteredErrors);
        model.addAttribute("jobId", jobId);
        model.addAttribute("searchUserId", userId); // 검색창에 입력한 ID 유지용

        return "batch-error-log-list";
    }
}
