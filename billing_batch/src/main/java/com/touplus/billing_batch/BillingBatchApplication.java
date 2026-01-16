package com.touplus.billing_batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class BillingBatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(BillingBatchApplication.class, args);
    }

}
