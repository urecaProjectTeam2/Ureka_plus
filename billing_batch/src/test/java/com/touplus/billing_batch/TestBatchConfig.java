package com.touplus.billing_batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.test.JobLauncherTestUtils; // 라이브러리 제공 클래스 임포트
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestBatchConfig {

    @Bean
    public JobLauncherTestUtils jobLauncherTestUtils(@Qualifier("billingJob") Job job) {
        JobLauncherTestUtils utils = new JobLauncherTestUtils();
        utils.setJob(job);
        return utils;
    }
}