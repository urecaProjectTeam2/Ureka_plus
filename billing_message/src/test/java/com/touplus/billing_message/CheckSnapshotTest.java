package com.touplus.billing_message;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;

import com.touplus.billing_message.consumer.BillingResultConsumer;
import com.touplus.billing_message.domain.dto.BillingResultDto;
import com.touplus.billing_message.domain.entity.BillingSnapshot;
import com.touplus.billing_message.domain.respository.BillingSnapshotRepository;

@SpringBootTest(properties = {
		 "spring.kafka.bootstrap-servers=localhost:9092", // 도커 Kafka
	    "spring.datasource.url=jdbc:mysql://localhost:3306/billing_message?useSSL=false&serverTimezone=Asia/Seoul",
	    "spring.datasource.username=root",
	    "spring.datasource.password=1234",
	    "spring.jpa.hibernate.ddl-auto=update"
	})

	class CheckSnapshotTest {

	    @Autowired
	    private KafkaTemplate<String, BillingResultDto> kafkaTemplate;

	    @Autowired
	    private BillingSnapshotRepository billingSnapshotRepository;

	    @Autowired
	    private BillingResultConsumer consumer;

	    @BeforeEach
	    void setup() {
	        billingSnapshotRepository.deleteAll(); // 테스트용 초기화
	    }

	    @TestConfiguration
	    static class KafkaTestConfig {
	        @Bean
	        public KafkaTemplate<String, BillingResultDto> kafkaTemplate() {
	            Map<String, Object> props = new HashMap<>();
	            props.put("bootstrap.servers", "localhost:9092");
	            props.put("key.serializer", StringSerializer.class);
	            props.put("value.serializer", JsonSerializer.class); // JSON 직렬화

	            return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
	        }
	    }
	    
	    @Test
	    void testDuplicateMessage() throws Exception {

	        BillingSnapshot existing = new BillingSnapshot(
	            1L,
	            LocalDate.of(2025, 12, 1),
	            1001L,
	            50000,
	            "{}"
	        );
	        billingSnapshotRepository.save(existing);

	        // 중복 확인
	        BillingResultDto message = new BillingResultDto();
	        message.setId(1L);
	        message.setUserId(1001L); // DB와 동일
	        message.setSettlementMonth(LocalDate.of(2025, 12, 1));
	        message.setTotalPrice(50000);

	        kafkaTemplate.send("billing-result", message); // 도커 Kafka로 전송
	        
	        Thread.sleep(1000); 
	        
	        List<BillingSnapshot> snapshots = billingSnapshotRepository.findAll();
	        assertEquals(1, snapshots.size());
	        assertEquals(1001L, snapshots.get(0).getUserId());
	        assertEquals(LocalDate.of(2025, 12, 1), snapshots.get(0).getSettlementMonth());
	    }
	}
