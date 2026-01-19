package com.touplus.billing_batch.config;

import com.touplus.billing_batch.domain.dto.BillingResultDto;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    private final KafkaProperties kafkaProperties;

    public KafkaProducerConfig(KafkaProperties kafkaProperties) {
        this.kafkaProperties = kafkaProperties;
    }

    @Bean
    public ProducerFactory<String, BillingResultDto> producerFactory() {

        Map<String, Object> props = kafkaProperties.buildProducerProperties(null);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        JsonSerializer<BillingResultDto> serializer = new JsonSerializer<>(objectMapper);
        serializer.setAddTypeInfo(true); // 타입 정보 포함

        return new DefaultKafkaProducerFactory<>(props, new StringSerializer(), serializer);
    }

    @Bean
    public KafkaTemplate<String, BillingResultDto> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
