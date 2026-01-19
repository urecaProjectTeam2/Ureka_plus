package com.touplus.billing_batch.jobs.billing;

import com.touplus.billing_batch.jobs.billing.step.listener.BillingSkipListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.transaction.PlatformTransactionManager;

import com.touplus.billing_batch.domain.dto.BillingCalculationResult;
import com.touplus.billing_batch.domain.entity.BillingUser;
import com.touplus.billing_batch.jobs.billing.step.reader.BillingItemReader;
import com.touplus.billing_batch.jobs.billing.step.processor.BillingItemProcessor;
import com.touplus.billing_batch.jobs.billing.step.writer.BillingItemWriter;


@Configuration
public class BillingJobConfig {

    @Bean
    public Step billingStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            BillingItemReader reader,
            BillingItemProcessor processor,
            BillingItemWriter writer,
            BillingSkipListener billingSkipListener // 리스너 주입
    ) {
        return new StepBuilder("billingStep", jobRepository)
                .<BillingUser, BillingCalculationResult>chunk(100, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(10)
                .listener(billingSkipListener) // 리스너 등록
                .build();
    }

    @Bean
    public Job billingJob(
            JobRepository jobRepository,
            Step billingStep
    ) {
        return new JobBuilder("billingJob", jobRepository)
                .start(billingStep)
                .build();
    }
}