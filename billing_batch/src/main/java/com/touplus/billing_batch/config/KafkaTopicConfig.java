package com.touplus.billing_batch.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic billingResultTopic() {
        return TopicBuilder.name("billing-result")
                .partitions(3)
                .replicas(1)
                .config(
                    TopicConfig.RETENTION_MS_CONFIG,
                    String.valueOf(3 * 24 * 60 * 60 * 1000L)
                )
                .build();
    }
}
