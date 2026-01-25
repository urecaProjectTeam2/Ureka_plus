package com.touplus.billing_batch.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/batch")
@RequiredArgsConstructor
public class BatchTestController {

    private final JobLauncher jobLauncher;
    private final Job billingJob;
    private final Job messageJob;

    /**
     * 전체 프로세스 테스트 (정산 후 메시지 발송까지)
     * GET /api/batch/all?targetMonth=2026-01-01
     */
    @GetMapping("/all")
    public String runFullProcess(@RequestParam String targetMonth) throws Exception {
        // 1. Billing Job 실행
        JobParameters billingParams = new JobParametersBuilder()
                .addString("targetMonth", targetMonth)
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        JobExecution billingExecution = jobLauncher.run(billingJob, billingParams);

        if (billingExecution.getStatus() == BatchStatus.COMPLETED) {
            // 2. Billing 성공 시 Message Job 실행
            JobParameters messageParams = new JobParametersBuilder()
                    .addString("targetMonth", targetMonth) // messageJob reader 쿼리에 쓰일 수도 있음
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(messageJob, messageParams);
            return "Full Batch Success: Billing -> Message";
        }

        return "Billing Batch Failed. Status: " + billingExecution.getStatus();
    }

    /**
     * 개별 Job 실행 (Billing만)
     */
    @GetMapping("/billing")
    public String runBilling(@RequestParam String targetMonth) throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addString("targetMonth", targetMonth)
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();
        jobLauncher.run(billingJob, params);
        return "Billing Job Started";
    }
}