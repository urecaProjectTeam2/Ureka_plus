package com.touplus.billing_batch.jobs.billing;

import com.touplus.billing_batch.common.BillingException;
import com.touplus.billing_batch.jobs.billing.step.listener.BillingJobListener;
import com.touplus.billing_batch.domain.dto.BillingUserBillingInfoDto;
import com.touplus.billing_batch.domain.entity.BillingResult;
import com.touplus.billing_batch.jobs.billing.partitioner.UserRangePartitioner;
import com.touplus.billing_batch.jobs.billing.step.listener.BillingSkipListener;
import com.touplus.billing_batch.jobs.billing.step.listener.BillingStepListener;
import com.touplus.billing_batch.jobs.billing.step.processor.AmountCalculationProcessor;
import com.touplus.billing_batch.jobs.billing.step.processor.DiscountCalculationProcessor;
import com.touplus.billing_batch.jobs.billing.step.processor.FinalBillingResultProcessor;
import com.touplus.billing_batch.jobs.billing.step.reader.BillingItemReader;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.CompositeItemProcessor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.ArrayList;
import java.util.List;


@Configuration
@RequiredArgsConstructor
public class BillingJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final UserRangePartitioner userRangePartitioner;

    private final BillingJobListener billingJobListener; // job 리스너 주입
    private final BillingSkipListener billingSkipListener; // 리스너 주입
    private final BillingStepListener billingStepListener; // 리스너 주입

    private final AmountCalculationProcessor amountCalculationProcessor;
    private final DiscountCalculationProcessor discountCalculationProcessor;
    private final FinalBillingResultProcessor unpaidAmountProcessor;

    @Bean
    public Job billingJob(@Qualifier("masterStep") Step masterStep) {
        return new JobBuilder("monthlyBillingJob", jobRepository)
                .start(masterStep)
                .listener(billingJobListener)
                .build();
    }

    @Bean
    public Step masterStep(@Qualifier("workerStep") Step workerStep) {  // 수정 : 함수 호출 대신에 bean 자체를 파라미터로 받고 호출함
        return new StepBuilder("masterStep", jobRepository)
                .partitioner("workerStep", userRangePartitioner)
                .step(workerStep) // 실제로 작업을 도는 step 설정이 들어있는 메소드
                .gridSize(10) // 10개의 파티션(스레드)에서 나누어 처리
                .taskExecutor(billingTaskExecutor())
                .build();
    }

    @Bean
    public Step workerStep(
            @Qualifier("billingItemWriter") ItemWriter<BillingResult> billingItemWriter, BillingItemReader billingItemReader
    ) { // 2. 여기서 직접 주입받음
        return new StepBuilder("workerStep", jobRepository)
                .<BillingUserBillingInfoDto, BillingResult>chunk(2000, transactionManager) // 청크 단위를 크게 가져가 성능 최적화 //데드락 수에 따라 조정 필요
                .reader(billingItemReader)
                .processor(compositeProcessor())
                .writer(billingItemWriter)  // JdbcBatchItemWriter 주입
                .faultTolerant()                 // 2. 내결함성 기능 활성화
                // --- 데드락 발생 시 재시도 로직 추가 ---
                .retry(org.springframework.dao.CannotAcquireLockException.class)
                .retryLimit(3) // 최대 3번까지 재시도
                // ------------------------------------
                .skip(BillingException.class)    // 3. 모든 예외에 대해 Skip 허용  --> 에러 발생 시 step 중단 없이 리스너가 가로챔
                .skip(DuplicateKeyException.class)  // 중복 키 에러도 스킵하도록 설정
                .skipLimit(1000)                   // 4. 최대 1000번까지 Skip 허용
                .listener(billingSkipListener)   // 5. 리스너 등록
                .listener(billingStepListener)
                .build();
    }

    @Bean(name = "billingTaskExecutor")
    public TaskExecutor billingTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10); // 기본 유지 스레드 수
        // 이거 왜 10으로 바꾼건지?
        executor.setMaxPoolSize(10); // 파티션 개수(gridSize)보다 약간 여유 있게 수정 // 최대 생성 가능 스레드 수 20 --> 15 --> 10
        executor.setQueueCapacity(100); // 큐를 설정하여 스레드 폭주 방지
        executor.setThreadNamePrefix("billing-thread-"); // 스레드 이름 앞에 붙는 접두가
        executor.initialize();
        return executor;
    }

    @Bean
    public CompositeItemProcessor<BillingUserBillingInfoDto, BillingResult> compositeProcessor() {
        List<ItemProcessor<?, ?>> delegates = new ArrayList<>();

        // 1. 단순 합산 프로세서
        delegates.add(amountCalculationProcessor);
        // 2. 할인 계산 프로세서
        delegates.add(discountCalculationProcessor);
        // 3. 미납금 계산 프로세서
        delegates.add(unpaidAmountProcessor);

        CompositeItemProcessor<BillingUserBillingInfoDto, BillingResult> processor = new CompositeItemProcessor<>();
        processor.setDelegates(delegates);
        return processor;
    }
}