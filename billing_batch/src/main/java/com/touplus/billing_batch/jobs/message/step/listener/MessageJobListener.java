package com.touplus.billing_batch.jobs.message.step.listener;

import com.touplus.billing_batch.domain.enums.ErrorType;
import com.touplus.billing_batch.domain.enums.JobType;
import com.touplus.billing_batch.domain.repository.service.BatchProcessRepository;
import com.touplus.billing_batch.jobs.billing.step.listener.BillingErrorLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
@JobScope
public class MessageJobListener implements JobExecutionListener {
    private final BatchProcessRepository batchProcessRepository;
    private final BillingErrorLogger errorLogger;

    @Value("#{jobParameters['settlementMonth']}")
    private String settlementMonth;

    private LocalDate startDate;
    private LocalDate endDate;

    @Override
    @Transactional
    public void beforeJob(JobExecution jobExecution) {
        try {
            // 1. yyMM 형식을 처리할 수 있는 포맷터 정의
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyMM");

            // 2. 정의한 포맷터를 사용하여 파싱
            YearMonth yearMonth = YearMonth.parse(settlementMonth, formatter);

            this.startDate = yearMonth.atDay(1);
            this.endDate = yearMonth.atEndOfMonth();

            batchProcessRepository.updateKafkaSent(JobType.PENDING, this.startDate, this.endDate);

        } catch (Exception e) {
            // 에러 발생 시 startDate와 endDate가 null일 수 있으므로 로직 점검이 필요할 수 있습니다.
            batchProcessRepository.updateKafkaSent(JobType.FAIL, this.startDate, this.endDate);
            errorLogger.saveForJobLevel(jobExecution, 0L, e, "BEFORE_JOB");
            throw e;
        }
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        // Job 종료 후 메모리 점유를 해제하기 위해 캐시를 비웁니다.
        if (jobExecution.getExitStatus().equals(ExitStatus.COMPLETED)) {
            batchProcessRepository.updateKafkaSent(JobType.DONE, this.startDate, this.endDate);
            batchProcessRepository.insertBatchProcess(startDate.plusMonths(1));
        } else {
            batchProcessRepository.updateKafkaSent(JobType.FAIL, this.startDate, this.endDate);

            if (!jobExecution.getAllFailureExceptions().isEmpty()) {
                Throwable t = jobExecution.getAllFailureExceptions().get(0);

                errorLogger.saveForKafkaJobLevel(jobExecution, 0L, t, "MESSAGE_AFTER_JOB", ErrorType.NETWORK);
            }
        }

        log.info(">>> [AfterKafkaJob] 배치가 종료되었습니다.");

    }

}
