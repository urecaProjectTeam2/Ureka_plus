package com.touplus.billing_batch.jobs.message;

import com.touplus.billing_batch.domain.dto.BillingResultDto;
import com.touplus.billing_batch.jobs.message.step.MessageSkipListener;
import com.touplus.billing_batch.jobs.message.step.MessageStepLogger;
import com.touplus.billing_batch.jobs.message.step.TopicCreateTasklet;
import com.touplus.billing_batch.jobs.message.step.writer.MessageItemWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
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
    private final MessageSkipListener messageSkipListener;
    private final MessageStepLogger messageStepLogger;
    private final TopicCreateTasklet topicCreateTasklet;
    private final int chunkSize = 1000;

    @Bean
    public Job messageJob(@Qualifier("createTopicStep") Step createTopicStep,
                          @Qualifier("messageJobStep") Step messageStepInstance) {
        return new JobBuilder("messageJob", jobRepository)
                .start(createTopicStep)      // 1. 토픽 생성 (TopicCreateTasklet 실행)
                .next(messageStepInstance)   // 2. 메시지 발송 실행
                .build();
    }

    // kafka topic 설정 담당 메소드
    @Bean(name = "createTopicStep")
    public Step createTopicStep() {
        return new StepBuilder("createTopicStep", jobRepository)
                .tasklet(topicCreateTasklet, transactionManager)
                .build();
    }

    // 메서드 이름을 messageStep에서 messageJobStep으로 변경 (중복 방지)
    @Bean(name = "messageJobStep")
    public Step messageJobStep(JdbcPagingItemReader<BillingResultDto> messageReader,
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
                .taskExecutor(messageTaskExecutor()) // 멀티스레드 적용
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
        executor.setCorePoolSize(20);
        executor.setMaxPoolSize(30);

        // [사용자 버전 채택] 비동기 Kafka 발송 작업이 밀릴 경우를 대비해 큐 용량을 넉넉히 설정 (10,000)
        executor.setQueueCapacity(10000);

        // [사용자 버전 채택] 스레드 이름 접두사 설정
        executor.setThreadNamePrefix("Msg-Thread-");

        executor.initialize();
        return executor;
    }
}