package com.touplus.billing_batch.scheduler;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class KafkaController {

    private final BillingKafkaScheduler billingKafkaScheduler;

    public KafkaController(BillingKafkaScheduler billingKafkaScheduler) {
        this.billingKafkaScheduler = billingKafkaScheduler;
    }

    @GetMapping("/publish")
    public String publish() {
        billingKafkaScheduler.sendBillingResult();
        log.info("컨트롤러 로그 솔직히 이건 찍혀야 함", "로그");
        return "Message sent to Kafka topic!";
    }
}
