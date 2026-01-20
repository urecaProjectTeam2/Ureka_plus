package com.touplus.billing_batch.jobs.billing.step.writer;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import com.touplus.billing_batch.domain.entity.BillingResult;
import com.touplus.billing_batch.domain.repository.BillingResultRepository;

@Component
@StepScope
@RequiredArgsConstructor
public class BillingItemWriter implements ItemWriter<BillingResult> {

    private final BillingResultRepository billingResultRepository;

    @PersistenceContext
    private final EntityManager entityManager;

    @Override
    public void write(Chunk<? extends BillingResult> items) throws Exception {
        billingResultRepository.saveAll((List<BillingResult>) items.getItems());
        entityManager.clear();
    }
}
