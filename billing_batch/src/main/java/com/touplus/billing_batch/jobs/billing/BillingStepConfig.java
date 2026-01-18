package com.touplus.billing_batch.jobs.billing;

import com.touplus.billing_batch.jobs.billing.step.writer.BillingItemWriter;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import org.springframework.batch.item.database.JpaPagingItemReader;

import com.touplus.billing_batch.domain.entity.BillingUser;
import com.touplus.billing_batch.jobs.billing.step.processor.BillingItemProcessor;
import com.touplus.billing_batch.jobs.billing.step.writer.BillingItemWriter;

@Configuration
public class BillingStepConfig {

    @Bean
    public Step billingStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            JpaPagingItemReader<BillingUser> billingItemReader,
            BillingItemProcessor processor,
            BillingItemWriter writer
    ) {
        return new StepBuilder("billingStep", jobRepository)
                .<BillingUser, BillingUser>chunk(1000, transactionManager)
                .reader(billingItemReader)
                .processor(processor)
                .writer(writer)
                .build();
    }
}
