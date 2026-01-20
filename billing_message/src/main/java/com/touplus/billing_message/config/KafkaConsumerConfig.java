package com.touplus.billing_message.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import com.touplus.billing_message.domain.dto.BillingResultDto;

@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    private final KafkaProperties kafkaProperties;

    public KafkaConsumerConfig(KafkaProperties kafkaProperties) {
        this.kafkaProperties = kafkaProperties;
    }

    @Bean public ConsumerFactory<String, BillingResultDto> consumerFactory() { 
    	
    	Map<String, Object> props = kafkaProperties.buildConsumerProperties(null); 
    	
    	return new DefaultKafkaConsumerFactory<>( 
    			props, 
    			new StringDeserializer(), 
    			new JsonDeserializer<>(BillingResultDto.class, false) 
    			); 
    	}
    
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, BillingResultDto> kafkaListenerContainerFactory(
            ConsumerFactory<String, BillingResultDto> consumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, BillingResultDto> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(true); 
        factory.setConcurrency(5);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

        return factory;
    }
}

