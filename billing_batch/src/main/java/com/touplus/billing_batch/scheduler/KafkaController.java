package com.touplus.billing_batch.scheduler;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class KafkaController {

    private final BillingKafkaScheduler billingKafkaScheduler;

    public KafkaController(BillingKafkaScheduler billingKafkaScheduler) {
        this.billingKafkaScheduler = billingKafkaScheduler;
    }

    @GetMapping("/publish")
    public String publish() {
        billingKafkaScheduler.runBillingKafkaJob();
        return "Message sent to Kafka topic!";
    }
}
