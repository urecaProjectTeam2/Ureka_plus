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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@EnableScheduling
@Component
@RequiredArgsConstructor
public class BillingKafkaScheduler {

    private final JobLauncher jobLauncher;

    @Qualifier("messageJob")
    private final Job messageJob; // 기존 message/batch 코드에서 정의된 Kafka 전송 Job

    // 예시 : @Scheduled(cron = "0 40 21 22 * ?")
    //                 22일 21시 40분 00초

//    @Scheduled(cron = "40 28 22 22 * ?") // 매월 2일 02시
    public void runBillingKafkaJob() {

        String settlementMonth = LocalDate.now()
                .minusMonths(1)
                .format(DateTimeFormatter.ofPattern("yyMM")); // "2512"

        try {
            // Job 파라미터 (중복 실행 방지를 위해 timestamp 추가)
            JobParameters params = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .addString("settlementMonth", settlementMonth)
                    .toJobParameters();

            // batch Job 실행
            jobLauncher.run(messageJob, params);

            log.info("[BillingKafkaScheduler] Kafka 전송 Job 실행 완료");

        } catch (JobInstanceAlreadyCompleteException e) {
            log.warn("[BillingKafkaScheduler] 이미 완료된 Kafka 전송 Job: {}", e.getMessage());
        } catch (JobExecutionAlreadyRunningException e) {
            log.error("[BillingKafkaScheduler] Kafka 전송 Job 이미 실행 중: {}", e.getMessage());
        } catch (JobParametersInvalidException e) {
            log.error("[BillingKafkaScheduler] 유효하지 않은 Job 파라미터: {}", e.getMessage());
        } catch (JobRestartException e) {
            log.error("[BillingKafkaScheduler] Job 재시작 실패: {}", e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("[BillingKafkaScheduler] Kafka 전송 Job 실행 실패", e);
        }
    }
}
