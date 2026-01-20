package com.touplus.billing_message.config;

import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class MessageDispatchConfig {

    @Bean
    public TaskExecutor messageDispatchTaskExecutor(
            @Value("${message.dispatch.pool-size:20}") int poolSize,
            @Value("${message.dispatch.queue-size:200}") int queueSize
    ) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize);
        executor.setQueueCapacity(queueSize);
        executor.setThreadNamePrefix("message-dispatch-");
        // 큐가 가득 차면 호출 스레드에서 직접 실행 (에러 방지)
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
