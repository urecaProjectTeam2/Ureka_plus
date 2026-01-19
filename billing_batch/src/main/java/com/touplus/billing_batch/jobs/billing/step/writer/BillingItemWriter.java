package com.touplus.billing_batch.jobs.billing.step.writer;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Builder;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.touplus.billing_batch.domain.dto.BillingCalculationResult;
import com.touplus.billing_batch.domain.entity.BillingResult;
import com.touplus.billing_batch.domain.enums.SendStatus;
import com.touplus.billing_batch.domain.repository.BillingResultRepository;

@Builder
@Component
public class BillingItemWriter
        implements ItemWriter<BillingCalculationResult> {

    private final BillingResultRepository resultRepository;
    private final ObjectMapper objectMapper;

    public BillingItemWriter(
            BillingResultRepository resultRepository,
            ObjectMapper objectMapper) {
        this.resultRepository = resultRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void write(Chunk<? extends BillingCalculationResult> items)
            throws Exception {

        for (BillingCalculationResult item : items) {

            String detailsJson = objectMapper.writeValueAsString(
                    item.getProducts()
            );

            BillingResult result = BillingResult.builder()
                    .userId(item.getUserId())
                    .settlementMonth(LocalDate.now().withDayOfMonth(1))
                    .totalPrice(item.getTotalPrice())
                    .settlementDetails(detailsJson)
                    .sendStatus(SendStatus.READY)
                    .batchExecutionId(0L) // JobExecutionListener에서 주입
                    .processedAt(LocalDateTime.now())
                    .build();

            resultRepository.save(result);
        }
    }
}
