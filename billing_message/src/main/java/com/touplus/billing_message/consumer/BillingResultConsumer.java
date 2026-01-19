package com.touplus.billing_message.consumer;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import com.touplus.billing_message.domain.dto.BillingResultDto;
import com.touplus.billing_message.domain.entity.BillingSnapshot;
import com.touplus.billing_message.domain.respository.BillingSnapshotJdbcRepository;
import com.touplus.billing_message.processor.MessageProcessor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillingResultConsumer {

    private final BillingSnapshotJdbcRepository jdbcRepository;
    private final MessageProcessor messageProcessor;


    @KafkaListener(
        topics = "billing-result",
        groupId = "billing-message-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            List<BillingResultDto> messages,
            Acknowledgment ack
    ) {
        try {
        	log.info("Kafka batch size={}", messages.size());
            LocalDate now = LocalDate.now();
            List<BillingSnapshot> toUpsert = new ArrayList<>();

            for (BillingResultDto message : messages) {
                LocalDate settlementMonth = message.getSettlementMonth();
                if (settlementMonth == null) continue;

                LocalDate processMonth = settlementMonth.plusMonths(1);
                if (processMonth.getYear() != now.getYear()
                    || processMonth.getMonth() != now.getMonth()) {
                    continue;
                }

                toUpsert.add(new BillingSnapshot(
                        message.getId(),
                        settlementMonth,
                        message.getUserId(),
                        message.getTotalPrice(),
                        message.getSettlementDetails() != null
                                ? message.getSettlementDetails().toString()
                                : "{}"
                ));
            }

            if (!toUpsert.isEmpty()) {
                jdbcRepository.batchUpsertByUserMonth(toUpsert);
                log.info("billing_snapshot upsert 요청={}건", toUpsert.size());

                // Message 생성 (각 snapshot에 대해 처리)
                for (BillingSnapshot snapshot : toUpsert) {
                    messageProcessor.process(snapshot);
                }
                log.info("Message 생성 완료={}건", toUpsert.size());
            }


            ack.acknowledge();

        } catch (Exception e) {
            log.error("Kafka batch 처리 실패", e);
            // ACK 안 함 → 재처리
        }
    }



    // Map에서 Long 가져오기
    private Long getLong(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) throw new IllegalArgumentException("필수 Long 값이 없음: " + key);
        if (value instanceof Integer) return ((Integer) value).longValue();
        if (value instanceof Long) return (Long) value;
        return Long.valueOf(value.toString());
    }

    // Map에서 Integer 가져오기
    private Integer getInt(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) throw new IllegalArgumentException("필수 Integer 값이 없음: " + key);
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Long) return ((Long) value).intValue();
        return Integer.valueOf(value.toString());
    }

    // Map에서 LocalDate 가져오기 (settlementMonth: [2025,12,1])
    private LocalDate getLocalDate(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            int year = ((Number) list.get(0)).intValue();
            int month = ((Number) list.get(1)).intValue();
            int day = ((Number) list.get(2)).intValue();
            return LocalDate.of(year, month, day);
        }
        throw new IllegalArgumentException("필수 LocalDate 값이 없음: " + key);
    }
}
