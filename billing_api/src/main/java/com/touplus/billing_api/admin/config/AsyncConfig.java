package com.touplus.billing_api.admin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig {

    @Bean(name = "sseTaskExecutor")
    public ThreadPoolTaskExecutor sseTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10); // 동시 모니터링 세션 수
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("BatchSSE-");
        executor.setWaitForTasksToCompleteOnShutdown(true); // 진행 중인 작업 완료 대기
        executor.setAwaitTerminationSeconds(60);            // 최대 60초 대기 후 강제 종료
        executor.initialize();
        return executor;
    }
}
