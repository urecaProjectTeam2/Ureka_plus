//package com.touplus.billing_batch.jobs.billing;
//
//import com.touplus.billing_batch.common.BillingException;
//import com.touplus.billing_batch.jobs.billing.step.listener.BillingSkipListener;
//import org.springframework.batch.core.Job;
//import org.springframework.batch.core.Step;
//import org.springframework.batch.core.job.builder.JobBuilder;
//import org.springframework.batch.core.repository.JobRepository;
//import org.springframework.batch.core.step.builder.StepBuilder;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import org.springframework.transaction.PlatformTransactionManager;
//
//import com.touplus.billing_batch.domain.dto.BillingCalculationResult;
//import com.touplus.billing_batch.domain.entity.BillingUser;
//import com.touplus.billing_batch.jobs.billing.step.reader.BillingItemReader;
//import com.touplus.billing_batch.jobs.billing.step.processor.BillingItemProcessor;
//import com.touplus.billing_batch.jobs.billing.step.writer.BillingItemWriter;
//
//
//@Configuration
//public class BillingJobConfig {
//
//    @Bean
//    public Step billingStep(
//            JobRepository jobRepository,
//            PlatformTransactionManager transactionManager,
//            BillingItemReader reader,
//            BillingItemProcessor processor,
//            BillingItemWriter writer,
//            BillingSkipListener billingSkipListener // 리스너 주입
//    ) {
//        return new StepBuilder("billingStep", jobRepository)
//                .<BillingUser, BillingCalculationResult>chunk(100, transactionManager)
//                .reader(reader)
//                .processor(processor)
//                .writer(writer)
//                .faultTolerant()                 // 2. 내결함성 기능 활성화
//                .skip(BillingException.class)           // 3. 모든 예외에 대해 Skip 허용  --> 에러 발생 시 step 중단 없이 리스너가 가로챔
//                .skipLimit(10)                   // 4. 최대 10번까지 Skip 허용
//                .listener(billingSkipListener)   // 5. 리스너 등록
//                .build();
//    }
//
//    @Bean
//    public Job billingJob(
//            JobRepository jobRepository,
//            Step billingStep
//    ) {
//        return new JobBuilder("billingJob", jobRepository)
//                .start(billingStep)
//                .build();
//    }
//}