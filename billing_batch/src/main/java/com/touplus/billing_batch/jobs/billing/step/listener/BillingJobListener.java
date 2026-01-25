package com.touplus.billing_batch.jobs.billing.step.listener;

import java.time.LocalDate;
import java.time.YearMonth;

import com.touplus.billing_batch.common.BillingFileRedirectionLogger;
import com.touplus.billing_batch.domain.enums.JobType;
import com.touplus.billing_batch.domain.repository.service.BatchProcessRepository;
import java.time.format.DateTimeFormatter;
import com.touplus.billing_batch.jobs.billing.cache.BillingReferenceLoader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Slf4j
@Component
@RequiredArgsConstructor
@JobScope
public class BillingJobListener implements JobExecutionListener {

    private final BillingErrorLogger errorLogger;
    private final BillingReferenceLoader billingReferenceLoader;
    private final BatchProcessRepository batchProcessRepository;
    private final BillingFileRedirectionLogger fileLogger;

    @Value("#{jobParameters['targetMonth']}")
    private String targetMonth;

    private LocalDate startDate;
    private LocalDate endDate;

    @Override
    @Transactional(readOnly = true) // 마스터 데이터 대량 조회 성능 최적화
    public void beforeJob(JobExecution jobExecution) {
        fileLogger.init();
        // 1. JobParameters에서 직접 꺼내오기 (가장 확실한 방법)
        String targetMonthStr = jobExecution.getJobParameters().getString("targetMonth");

        fileLogger.write(">>> 정산 대상 월 (Param): " + targetMonthStr);

        try {
            // null 체크 추가
            if (targetMonthStr == null) {
                throw new IllegalArgumentException("targetMonth 파라미터가 null입니다. Job 실행 시 파라미터를 확인하세요.");
            }

            // 2. 파싱 로직 (입력: "2025-12-01")
            LocalDate parsedDate = LocalDate.parse(targetMonthStr); // yyyy-MM-dd 형식
            YearMonth ym = YearMonth.from(parsedDate);

            this.startDate = ym.atDay(1);
            this.endDate = ym.atEndOfMonth();

            batchProcessRepository.updateJob(JobType.PENDING, this.startDate, this.endDate);
            billingReferenceLoader.loadOrThrow();

        } catch (Exception e) {
            batchProcessRepository.updateJob(JobType.FAIL, this.startDate, this.endDate);
            errorLogger.saveForJobLevel(jobExecution, 0L, e, "BEFORE_JOB");
            throw e;
        }
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        // Job 종료 후 메모리 점유를 해제하기 위해 캐시를 비웁니다.
        if (jobExecution.getExitStatus().equals(ExitStatus.COMPLETED)) {
            batchProcessRepository.updateJob(JobType.DONE, this.startDate, this.endDate);
        } else {
            batchProcessRepository.updateJob(JobType.FAIL, this.startDate, this.endDate);
        }

        log.info(">>> [AfterJob] 배치가 종료되어 캐시 데이터를 정리합니다.");
        billingReferenceLoader.clear();

        fileLogger.write(">>> [AfterJob] 최종 실행 상태 : "+ jobExecution.getExitStatus());
        fileLogger.close();
    }
}