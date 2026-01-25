package com.touplus.billing_batch.jobs.billing.step.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.touplus.billing_batch.common.BillingException;
import com.touplus.billing_batch.common.BillingFatalException;
import com.touplus.billing_batch.domain.dto.BillingWorkDto;
import com.touplus.billing_batch.domain.dto.SettlementDetailsDto;
import com.touplus.billing_batch.domain.dto.SettlementDetailsDto.DetailItem;
import com.touplus.billing_batch.domain.entity.BillingResult;
import com.touplus.billing_batch.domain.enums.SendStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class FinalBillingResultProcessor
        implements ItemProcessor<BillingWorkDto, BillingResult> {

    private final ObjectMapper objectMapper;

    @Value("#{stepExecution.jobExecutionId}")
    private Long jobExecutionId;

    @Value("#{jobParameters['targetMonth']}")
    private String targetMonth;

    @Override
    public BillingResult process(BillingWorkDto work) throws Exception {

        // 10원 미만 처리
        long totalSum = (int)(work.getTotalPrice());
        int underTen = (int)(totalSum % 10);
        int totalPrice = (int)(totalSum - underTen);

        DetailItem detailUnderTen = DetailItem.builder()
                .productName("10원 미만 할인")
                .productType("DISCOUNT_UNDERTEN")
                .price(underTen * -1)
                .build();

        work.getDiscounts().add(detailUnderTen);

        // 상세 내역 구성
        SettlementDetailsDto settlementDetailsDto = SettlementDetailsDto.builder()
                .mobile(work.getMobile())
                .internet(work.getInternet())
                .iptv(work.getIptv())
                .dps(work.getDps())
                .addon(work.getAddon())
                .discounts(work.getDiscounts())
                .build();

        String detailsJson = objectMapper.writeValueAsString(settlementDetailsDto);

        return BillingResult.builder()
                .userId(work.getRawData().getUserId())
                .settlementMonth(LocalDate.parse(targetMonth))
                .totalPrice(totalPrice)
                .settlementDetails(detailsJson)
                .sendStatus(SendStatus.READY)
                .batchExecutionId(jobExecutionId)
                .processedAt(LocalDateTime.now())
                .build();
    }
}
