package com.touplus.billing_batch.scheduler;

import com.touplus.billing_batch.domain.BillingResult;
import com.touplus.billing_batch.domain.BillingResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

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
        List<BillingResult> targets = billingResultRepository.findReadyForSend(PageRequest.of(0, 10));

        for (BillingResult billing : targets) {
            try {
                log.info("Trying to send billingResultId={}", billing.getId());
                log.info("보내는 메시지: {}", billing);
                billing.markSending();

                kafkaTemplate.send(
                        TOPIC,
                        billing.getId().toString(), // Kafka Key
                        billing
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
