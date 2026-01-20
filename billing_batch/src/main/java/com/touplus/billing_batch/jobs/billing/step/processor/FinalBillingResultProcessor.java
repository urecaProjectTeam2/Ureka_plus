package com.touplus.billing_batch.jobs.billing.step.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.touplus.billing_batch.domain.dto.BillingUserBillingInfoDto;
import com.touplus.billing_batch.domain.dto.BillingWorkDto;
import com.touplus.billing_batch.domain.dto.SettlementDetailsDto;
import com.touplus.billing_batch.domain.dto.SettlementDetailsDto.DetailItem;
import com.touplus.billing_batch.domain.dto.UnpaidDto;
import com.touplus.billing_batch.domain.entity.BillingResult;
import com.touplus.billing_batch.domain.enums.SendStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@StepScope
@RequiredArgsConstructor
public class FinalBillingResultProcessor
        implements ItemProcessor<BillingWorkDto, BillingResult> {

    private final ObjectMapper objectMapper;

    @Value("#{stepExecution.jobExecutionId}")
    private Long jobExecutionId;

    @Override
    public BillingResult process(BillingWorkDto work) throws Exception {
        // 미납금 합산
        int totalUnpaid = 0;
        for (UnpaidDto u : work.getRawData().getUnpaids()) {
            totalUnpaid += u.getUnpaidPrice();
            work.getUnpaids().add(DetailItem.builder()
                    .productType("UNPAID")
                    .productName(u.getUnpaidMonth() + "미납금")
                    .price(u.getUnpaidPrice())
                    .build());
        }

        // 최종 청구 금액 계산 (상품 + 추가요금 - 할인 + 미납금)
        int finalPrice = work.getTotalPrice() + totalUnpaid;

        // 상세 내역 구성
        SettlementDetailsDto settlementDetailsDto = SettlementDetailsDto.builder()
                .mobile(work.getMobile())
                .internet(work.getInternet())
                .iptv(work.getIptv())
                .dps(work.getDps())
                .addon(work.getAddon())
                .discounts(work.getDiscounts())
                .unpaids(work.getUnpaids())
                .build();

        String detailsJson = objectMapper.writeValueAsString(settlementDetailsDto);

        return BillingResult.builder()
                .userId(work.getRawData().getUserId())
                .settlementMonth(LocalDate.now().withDayOfMonth(1))
                .totalPrice(finalPrice)
                .settlementDetails(detailsJson)
                .sendStatus(SendStatus.READY)
                .batchExecutionId(jobExecutionId)
                .processedAt(LocalDateTime.now())
                .build();
    }
}
