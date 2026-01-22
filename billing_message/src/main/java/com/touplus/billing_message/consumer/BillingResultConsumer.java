package com.touplus.billing_message.consumer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import com.touplus.billing_message.domain.dto.BillingResultDto;
import com.touplus.billing_message.domain.entity.BillingSnapshot;
import com.touplus.billing_message.domain.entity.User;
import com.touplus.billing_message.domain.respository.BillingSnapshotJdbcRepository;
import com.touplus.billing_message.domain.respository.UserRepository;
import com.touplus.billing_message.processor.MessageProcessor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillingResultConsumer {

    private final BillingSnapshotJdbcRepository jdbcRepository;
    private final UserRepository userRepository;
    private final MessageProcessor messageProcessor;

    @KafkaListener(
    		topics = "billing-result-topic-2512T2", 
    		groupId = "billing-message-groupM16", 
    		containerFactory = "kafkaListenerContainerFactory")
    public void consume(
            List<BillingResultDto> messages,
            Acknowledgment ack) {
        try {
            LocalDate now = LocalDate.now();
            log.info("Kafka 메시지 수신: {}건, 시작 시각: {}", messages.size(), LocalDateTime.now());

            List<BillingSnapshot> toUpsert = new ArrayList<>();

            for (BillingResultDto message : messages) {
                LocalDate settlementMonth = message.getSettlementMonth();

                if (settlementMonth == null)
                    continue;

                LocalDate processMonth = settlementMonth.plusMonths(1);
                if (processMonth.getYear() != now.getYear()
                   /* || processMonth.getMonth() != now.getMonth() 
                    || !"SUCCESS".equals(message.getSendStatus())) {
                    continue;
                }*/
                
                // 스냅샷 db 저장
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
                                : "{}"));
            }

            // BillingSnapshot 저장 (1000건씩 배치)
            int batchSize = 1000;
            
            for (int i = 0; i < toUpsert.size(); i += batchSize) {
                int end = Math.min(i + batchSize, toUpsert.size());
                jdbcRepository.batchUpsertByUserMonth(toUpsert.subList(i, end));
            }

            // 스냅샷 카운트 확인 (JDBC)
            long snapshotCount = jdbcRepository.count();
            if (snapshotCount >= 9999L) {
                log.info("스냅샷 완료! count={}", snapshotCount);
                log.info("Message 처리 시작: {}", LocalDateTime.now());

                // 1. JDBC로 전체 스냅샷 조회 (JPA 페이징 오버헤드 제거)
                List<BillingSnapshot> allSnapshots = jdbcRepository.findAll();
                log.info("스냅샷 조회 완료: {}건", allSnapshots.size());

                // 2. User 한 번에 조회 (10000건 → 1회 쿼리)
                List<Long> allUserIds = allSnapshots.stream()
                        .map(BillingSnapshot::getUserId)
                        .distinct()
                        .toList();
                Map<Long, User> userMap = userRepository.findAllById(allUserIds).stream()
                        .collect(Collectors.toMap(User::getUserId, u -> u));
                log.info("User 조회 완료: {}건", userMap.size());

                // 3. 청크로 분할
                int chunkSize = 1000;
                List<List<BillingSnapshot>> chunks = new ArrayList<>();
                for (int i = 0; i < allSnapshots.size(); i += chunkSize) {
                    int end = Math.min(i + chunkSize, allSnapshots.size());
                    chunks.add(allSnapshots.subList(i, end));
                }

                // 4. 병렬 처리 (10 스레드)
                int parallelCount = 10;
                java.util.concurrent.ExecutorService executor =
                        java.util.concurrent.Executors.newFixedThreadPool(parallelCount);

                List<java.util.concurrent.Future<?>> futures = new ArrayList<>();
                for (List<BillingSnapshot> chunk : chunks) {
                    futures.add(executor.submit(() ->
                            messageProcessor.processBatchWithUsers(chunk, userMap)));
                }

                // 완료 대기
                for (java.util.concurrent.Future<?> future : futures) {
                    try {
                        future.get();
                    } catch (Exception e) {
                        log.error("청크 처리 실패", e);
                    }
                }

                executor.shutdown();
                log.info("Message 처리 완료: {}", LocalDateTime.now());
            }

            ack.acknowledge();
        } catch (Exception e) {
            log.error("Kafka batch 처리 실패", e);
        }
    }
}
