package com.touplus.billing_batch.jobs.billing.step.reader;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.touplus.billing_batch.domain.entity.BillingUser;

@Configuration
public class BillingItemReader {

    @Bean
    public JpaPagingItemReader<BillingUser> billingItemReader(
            EntityManagerFactory emf
    ) {
        return new JpaPagingItemReaderBuilder<BillingUser>()
                .name("billingItemReader")
                .entityManagerFactory(emf)
                .queryString("""
                    SELECT u
                    FROM BillingUser u
                    ORDER BY u.userId
                """)
                .pageSize(1000)
                .build();
    }
}
