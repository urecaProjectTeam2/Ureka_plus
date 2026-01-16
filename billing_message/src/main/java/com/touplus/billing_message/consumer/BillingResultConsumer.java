package com.touplus.billing_message.consumer;

import com.touplus.billing_message.domain.dto.BillingResultMessage;
import com.touplus.billing_message.domain.entity.BillingSnapshot;
import com.touplus.billing_message.domain.respository.BillingSnapshotRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Component
@RequiredArgsConstructor
public class BillingResultConsumer {

    private final BillingSnapshotRepository billingSnapshotRepository;

    @KafkaListener(
    topics = "billing-result",
    groupId = "billing-message-group"
    )
    @Transactional
    public void consume(BillingResultMessage message) {
        try {
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

        } catch (DataIntegrityViolationException e) {
            log.info("중복 Kafka 메시지 무시");
        }
    }
}
