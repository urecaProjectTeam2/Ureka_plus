package com.touplus.billing_batch.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/batch")
@RequiredArgsConstructor
public class BillingBatchController {

    private final BillingBatchScheduler billingBatchScheduler;

    @GetMapping("/billing")
    public ResponseEntity<String> runBillingJob(
            @RequestParam(required = false) String settlementMonth
    ) throws Exception {

        log.info("[Get batch/billing] 배치 수동 호출");

        billingBatchScheduler.runMonthlyBilling();

        return ResponseEntity.ok(
                "[Get batch/billing] Billing batch started."
        );
    }
}