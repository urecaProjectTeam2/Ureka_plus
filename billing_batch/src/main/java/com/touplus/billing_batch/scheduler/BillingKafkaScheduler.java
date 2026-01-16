package com.touplus.billing_batch.scheduler;

import com.touplus.billing_batch.domain.dto.BillingResultMessage;
import com.touplus.billing_batch.domain.entity.BillingResult;
import com.touplus.billing_batch.domain.entity.SendStatus;
import com.touplus.billing_batch.domain.repository.BillingResultRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillingKafkaScheduler {

    private final BillingResultRepository billingResultRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC = "billing-result";

    @Scheduled(fixedDelay = 1000) // 예시를 위한 1초
    @Transactional
    public void sendBillingResult() {
        List<BillingResult> targets = billingResultRepository.findBySendStatusOrderById(SendStatus.READY);

        for (BillingResult billing : targets) {
            try {
                log.info("Trying to send billingResultId={}", billing.getId());

                // Entity → DTO 변환
                BillingResultMessage message = new BillingResultMessage();
                message.setId(billing.getId());
                message.setSettlementMonth(billing.getSettlementMonth());
                message.setUserId(billing.getUserId());
                message.setTotalPrice(billing.getTotalPrice());
                message.setSettlementDetails(billing.getSettlementDetails());
                message.setSendStatus(billing.getSendStatus().name());
                message.setBatchExecutionId(billing.getBatchExecutionId());
                message.setProcessedAt(billing.getProcessedAt());

                billing.markSending();

                kafkaTemplate.send(
                        TOPIC,
                        billing.getId().toString(), // Kafka Key
                        message                    //  DTO 전송
                ).get(); // 동기 전송

                billing.markSuccess();
                log.info("Successfully sent billingResultId={}", billing.getId());
            } catch (Exception e) {
                log.error("Kafka 전송 실패 billingResultId={}", billing.getId(), e);
                billing.markFail();
            }
        }
    }
}
