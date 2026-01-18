package com.touplus.billing_message.consumer;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.touplus.billing_message.domain.dto.BillingResultMessage;
import com.touplus.billing_message.domain.entity.BillingSnapshot;
import com.touplus.billing_message.domain.respository.BillingSnapshotRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillingResultConsumer {

    private final BillingSnapshotRepository billingSnapshotRepository;

    @KafkaListener(
        topics = "billing-result",
        groupId = "billing-message-group"
    // 수동 커밋을 위해 container factory에서 AckMode MANUAL 설정 필요 - 나중에 여기에 적으면 될 듯
    )
    @Transactional
    public void consume(BillingResultMessage message, Acknowledgment ack) {

        try {
        	// 메시지 정구 달이 현재 시점 기준으로 한 달 전이어야 함, ex) 청구 달 :12, 현재 처리 달:1
        	 LocalDate messageMonth =  message.getSettlementMonth().plusMonths(1); 
             LocalDate now = LocalDate.now();
             
             // 데이터를 처리하는 달이 아니면 return
             if (messageMonth.getYear() != now.getYear() || messageMonth.getMonth() != now.getMonth()) {
                 log.info(messageMonth + "가 아닌 달", message.getId());
                 ack.acknowledge();
                 return;
             }
             
            boolean exists = billingSnapshotRepository.existsByUserIdAndSettlementMonth(
                message.getUserId(), message.getSettlementMonth()
            );

            if (exists) { // snapshot에 중복 데이터가 있으면
                log.info("snapshot에 중복 데이터 있음", message.getId());
                ack.acknowledge();
                return;          
            }
        	
            BillingSnapshot snapshot = new BillingSnapshot(
                    message.getId(),
                    message.getSettlementMonth(),
                    message.getUserId(),
                    message.getTotalPrice(),
                    message.getSettlementDetails() != null
                            ? message.getSettlementDetails().toString() : "{}"
            );

            billingSnapshotRepository.save(snapshot);
            log.info("billing_snapshot 저장 완료 billingId={}", snapshot.getBillingId());
            ack.acknowledge();

        } catch (Exception e) {
            // db 실패 등 처리 에러가 날 경우
            log.error("Kafka 메시지 처리 실패 billingId={}", message.getId(), e);
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
