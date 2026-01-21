package com.touplus.billing_batch.jobs.message;

import com.touplus.billing_batch.domain.dto.BillingResultDto;
import com.touplus.billing_batch.jobs.message.step.MessageSkipListener;
import com.touplus.billing_batch.jobs.message.step.MessageStepLogger;
import com.touplus.billing_batch.jobs.message.step.writer.MessageItemWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class MessageJobConfig {

    /*
     *               송 신 전 략
     * 1. single x
     * 2. Multi-threads o
     *           --> 다른 chunk를 사용하도록 설정 --> 중복된 데이터 송신 걱정 x
     */



    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    //  1. 필드 주입 제거 (순환 고리 끊기)
    // private final MessageItemWriter messageItemWriter;
    private final MessageSkipListener messageSkipListener;
    private final MessageStepLogger messageStepLogger;
    private final int chunkSize = 1000;

    @Bean
    public Job messageJob(@Qualifier("messageJobStep")Step messageStepInstance) { // 파라미터명 변경
        return new JobBuilder("messageJob", jobRepository)
                .start(messageStepInstance)
                .build();
    }

    // 메서드 이름을 messageStep에서 messageJobStep으로 변경 (중복 방지)
    @Bean(name = "messageJobStep")
    public Step messageJobStep(JdbcPagingItemReader<BillingResultDto> messageReader, // 타입 변경
                               MessageItemWriter messageItemWriter) {
        return new StepBuilder("messageJobStep", jobRepository)
                .<BillingResultDto, BillingResultDto>chunk(chunkSize, transactionManager)
                .reader(messageReader)
                .writer(messageItemWriter)
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(1000)
                .listener(messageStepLogger)
                .listener(messageSkipListener)
                .taskExecutor(messageTaskExecutor()) //= =스레드 세이프 하게 돌아가도록
                .build();
    }

    @Bean
    public RetryTemplate retryTemplate() {
        return RetryTemplate.builder()
                .maxAttempts(3)
                .exponentialBackoff(100, 2.0, 1000)
                .build();
    }

    @Bean(name = "messageTaskExecutor")
    public TaskExecutor messageTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);   // 서버 사양에 따라서 수정 ( 20~30)
        executor.setMaxPoolSize(30);
        executor.setQueueCapacity(2000);
        executor.setThreadNamePrefix("Batch-Thread-");
        executor.initialize();
        return executor;
    }
}


