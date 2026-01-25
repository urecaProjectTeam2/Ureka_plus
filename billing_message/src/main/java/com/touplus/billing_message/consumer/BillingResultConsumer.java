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
import com.touplus.billing_message.service.KafkaInputTracker;
import com.touplus.billing_message.service.BatchClosureScheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillingResultConsumer {

    private final BillingSnapshotJdbcRepository jdbcRepository;
    private final UserRepository userRepository;
    private final MessageProcessor messageProcessor;
    private final KafkaInputTracker kafkaInputTracker;
    private final BatchClosureScheduler batchClosureScheduler;

    @KafkaListener(
    		topics = "billing-result-topic-2512", 
    		groupId = "billing-message-group-JH111-12", 
    		containerFactory = "kafkaListenerContainerFactory")
    public void consume(
            List<BillingResultDto> messages,
            Acknowledgment ack) {
        try {
            // 입력 추적 + 배치 상태 리셋
            kafkaInputTracker.recordInput();
            batchClosureScheduler.resetBatchClosure();
            
            LocalDate now = LocalDate.now();
            log.info("Kafka 메시지 수신: {}건, 시작 시각: {}", messages.size(), LocalDateTime.now());

            // 1. 메모리에서 BillingSnapshot 리스트 생성
            List<BillingSnapshot> toProcess = new ArrayList<>();

            for (BillingResultDto message : messages) {
                LocalDate settlementMonth = message.getSettlementMonth();
                if (settlementMonth == null) continue;

                LocalDate processMonth = settlementMonth.plusMonths(1);
                if (processMonth.getYear() != now.getYear()
                        || processMonth.getMonth() != now.getMonth()) {
                    continue;
                }

                toProcess.add(new BillingSnapshot(
                        message.getId(),
                        settlementMonth,
                        message.getUserId(),
                        message.getTotalPrice(),
                        message.getSettlementDetails() != null
                                ? message.getSettlementDetails().toString()
                                : "{}"));
            }

            if (toProcess.isEmpty()) {
                ack.acknowledge();
                return;
            }

            log.info("유효 데이터: {}건", toProcess.size());

            // 2. [로그용] BillingSnapshot 테이블에 저장 (처리 로직과 독립)
            int batchSize = 1000;
            for (int i = 0; i < toProcess.size(); i += batchSize) {
                int end = Math.min(i + batchSize, toProcess.size());
                jdbcRepository.batchUpsertByUserMonth(toProcess.subList(i, end));
            }

            // 3. User 일괄 조회 (이번 배치에서 필요한 유저만)
            List<Long> userIds = toProcess.stream()
                    .map(BillingSnapshot::getUserId)
                    .distinct()
                    .toList();
            Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                    .collect(Collectors.toMap(User::getUserId, u -> u));
            log.info("User 조회 완료: {}건", userMap.size());

            // 4. 받은 데이터를 즉시 Message로 변환하여 저장 (단일 스레드, 순차 처리)
            messageProcessor.processBatchWithUsers(toProcess, userMap);

            log.info("Message 처리 완료: {}, 처리 건수: {}", LocalDateTime.now(), toProcess.size());

            ack.acknowledge();
        } catch (Exception e) {
            log.error("Kafka batch 처리 실패", e);
        }
    }
}
