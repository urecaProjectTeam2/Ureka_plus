package com.touplus.billing_batch.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@EnableScheduling
@Component
@RequiredArgsConstructor
public class BillingBatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job billingJob;

//    @Scheduled(cron = "0 0 2 1 * ?") // 매월 1일 02시
    public void runMonthlyBilling() {

        // 정산 대상이 되는 월 계산. 정산 해당 월 1일로 계산
//        String targetMonth = LocalDate.now().minusMonths(1).withDayOfMonth(1).toString();
        String targetMonth = LocalDate.of(2025, 12, 1).toString();

        try{
            JobParameters params = new JobParametersBuilder()
                    .addString("targetMonth", targetMonth)
                    .addLong("time", System.currentTimeMillis()) // 배치 중복 실행 가능하게
                    .toJobParameters();
            // =================> 수동 재실행을 어떻게 가능하게 할지 고민.

            jobLauncher.run(billingJob, params);

        } catch (JobInstanceAlreadyCompleteException e) {
            log.warn("[BillingBatchScheduler] 이미 완료된 정산 작업입니다: {}", e.getMessage());
        } catch (JobExecutionAlreadyRunningException e) {
            log.error("[BillingBatchScheduler] 배치가 이미 실행 중입니다: {}", e.getMessage());
        } catch (JobParametersInvalidException e) {
            log.error("[BillingBatchScheduler] 유효하지 않은 파라미터입니다: {}", e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("[BillingBatchScheduler] 정산 배치 가동 실패", e);
        }
    }
}
