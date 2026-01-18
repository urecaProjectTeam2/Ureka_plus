package com.touplus.billing_message;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;

import com.touplus.billing_message.consumer.BillingResultConsumer;
import com.touplus.billing_message.domain.dto.BillingResultMessage;
import com.touplus.billing_message.domain.entity.BillingSnapshot;
import com.touplus.billing_message.domain.respository.BillingSnapshotRepository;

@SpringBootTest
@EmbeddedKafka(
    partitions = 1,
    topics = { "billing-result" },
    brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" }
)

class CheckSnapshotTest {

    @Autowired
    private KafkaTemplate<String, BillingResultMessage> kafkaTemplate;

    @Autowired
    private BillingSnapshotRepository billingSnapshotRepository;

    @Autowired
    private BillingResultConsumer consumer;

    @BeforeEach
    void setup() {
        billingSnapshotRepository.deleteAll(); // 테스트용 초기화
    }

    @Test
    void testDuplicateMessage() throws Exception {
        // 1️⃣ DB에 중복 데이터 미리 생성
        BillingSnapshot existing = new BillingSnapshot(
            1L,
            LocalDate.of(2025, 12, 1),
            1001L,
            50000,
            "{}"
        );
        billingSnapshotRepository.save(existing);

        // 2️⃣ Kafka 메시지 생성 (중복)
        BillingResultMessage message = new BillingResultMessage();
        message.setId(2L);
        message.setUserId(1001L); // DB와 동일
        message.setSettlementMonth(LocalDate.of(2025, 12, 1));
        message.setTotalPrice(50000);

        // 3️⃣ Kafka로 메시지 전송
        kafkaTemplate.send("billing-result", message.getId().toString(), message).get();

        // 4️⃣ Consumer가 메시지를 처리할 시간을 잠깐 기다림
        Thread.sleep(1000);

        // 5️⃣ DB 상태 확인: 기존 데이터 그대로, 새로운 데이터는 추가되지 않음
        List<BillingSnapshot> snapshots = billingSnapshotRepository.findAll();
        assertEquals(1, snapshots.size());
        assertEquals(1001L, snapshots.get(0).getUserId());

        // 6️⃣ 로그 확인 (선택) - 중복 메시지 무시 로그
    }
}
