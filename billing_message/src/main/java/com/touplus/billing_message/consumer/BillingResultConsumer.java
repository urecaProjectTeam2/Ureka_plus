package com.touplus.billing_message.consumer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import com.touplus.billing_message.domain.dto.BillingResultDto;
import com.touplus.billing_message.domain.entity.BillingSnapshot;
import com.touplus.billing_message.domain.respository.BillingSnapshotJdbcRepository;
import com.touplus.billing_message.domain.respository.BillingSnapshotRepository;
import com.touplus.billing_message.processor.MessageProcessor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillingResultConsumer {

    private final BillingSnapshotJdbcRepository jdbcRepository;
    private final BillingSnapshotRepository bsr;
    private final MessageProcessor messageProcessor;


    @KafkaListener(
        topics = "billing-result",
        groupId = "billing-message-group",
        containerFactory = "kafkaListenerContainerFactory" // offset 수동 저장
    )
    public void consume(
            List<BillingResultDto> messages,
            Acknowledgment ack // offset
    ) {         
        try {
        	long filterStart = System.currentTimeMillis();
            LocalDate now = LocalDate.now();
            log.info("데이터 넣기 시작 시각 : {}", LocalDateTime.now());
        	
            List<BillingSnapshot> toUpsert = new ArrayList<>();

            for (BillingResultDto message : messages) {
                LocalDate settlementMonth = message.getSettlementMonth();
                
                if (settlementMonth == null) continue;

                // 청구 월이 현재 시간의 월보다 -1 되어야 함 ex) 청구 : 12월, 지불 : 1월
                LocalDate processMonth = settlementMonth.plusMonths(1);
                if (processMonth.getYear() != now.getYear()
                    || processMonth.getMonth() != now.getMonth()) {
                    continue;
                }
                // 중복 체크는 INSERT IGNORE가 자동 처리

                // 스냅샷 db 저장
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
            
            // 배치 사이즈 최적화
            int batchSize = 1000;
            
            // ① Phase 1: 스냅샷 저장 (INSERT IGNORE)
            for (int i = 0; i < toUpsert.size(); i += batchSize) {
                int end = Math.min(i + batchSize, toUpsert.size());
                jdbcRepository.batchUpsertByUserMonth(toUpsert.subList(i, end));
            }
            
            // ② Phase 2: 스냅샷 완료 후 Message 일괄 처리
            Long snapshotCount = bsr.countAll();
            if (snapshotCount >= 10000L) {  // 테스트용 - 나중에 조건 변경 가능
                log.info("스냅샷 데이터 다 넣음! count={}", snapshotCount);
                log.info("Message 처리 시작 시각 : {}", LocalDateTime.now());
                
                // DB에서 페이징으로 스냅샷 조회 후 Message 처리 (Chunk 방식)
                int messageBatchSize = 1000;
                int pageNum = 0;
                Page<BillingSnapshot> page;
                
                do {
                    page = bsr.findAll(PageRequest.of(pageNum++, messageBatchSize));
                    // 청크 처리 (Processor 내부에서 누락 감지 및 재시도)
                    messageProcessor.processBatch(page.getContent());
                } while (page.hasNext());
                
                log.info("Message 처리 완료 시각 : {}", LocalDateTime.now());
            }
            
            // ③ Kafka 커밋
            ack.acknowledge();
        } catch (Exception e) {
        	// 중복 제외 기타 오류 발생시
            log.error("Kafka batch 처리 실패", e);
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
