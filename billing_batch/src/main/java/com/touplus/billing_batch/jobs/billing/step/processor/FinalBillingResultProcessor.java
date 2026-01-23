package com.touplus.billing_batch.jobs.billing.step.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.touplus.billing_batch.common.BillingException;
import com.touplus.billing_batch.common.BillingFatalException;
import com.touplus.billing_batch.domain.dto.BillingWorkDto;
import com.touplus.billing_batch.domain.dto.SettlementDetailsDto;
import com.touplus.billing_batch.domain.dto.SettlementDetailsDto.DetailItem;
import com.touplus.billing_batch.domain.dto.UnpaidDto;
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
        // Unpaid 리스트 초기화
        if (work.getUnpaids() == null) {
            work.setUnpaids(new ArrayList<>());
        }


        // 미납금이 없으면 빈 리스트 처리
        List<UnpaidDto> unpaids =
                work.getRawData().getUnpaids() == null ? Collections.emptyList() : work.getRawData().getUnpaids();

        // 미납금 합산
        int totalUnpaid = 0;
        for (UnpaidDto u : unpaids) {
            // 데이터 이상
            if (u == null) {
                throw BillingException.dataNotFound(work.getRawData().getUserId(), "미납 Row가 비어있습니다.");
            }

            if (u.getUnpaidMonth() == null) {
                throw BillingException.dataNotFound(work.getRawData().getUserId(), "미납 월이 비어있습니다.");
            }

            YearMonth targetYM = YearMonth.from(LocalDate.parse(targetMonth));
            if(YearMonth.from(u.getUnpaidMonth()).isAfter(targetYM)){
                throw BillingException.dataNotFound(work.getRawData().getUserId(), "미납 월이 정산 기준 월 이후입니다.");
            }

            int unpaidPrice = u.getUnpaidPrice();

            if (unpaidPrice < 0) {
                throw BillingException.invalidUnpaidAmount(work.getRawData().getUserId(), String.valueOf(u.getUnpaidId()));
            }

            totalUnpaid += unpaidPrice;
            work.getUnpaids().add(DetailItem.builder()
                    .productType("UNPAID")
                    .productName(u.getUnpaidMonth() + "미납금")
                    .price(u.getUnpaidPrice())
                    .build());
        }
//        log.info("[FinalBillingResultProcessor] 미납금 합산 완료");

        // 최종 청구 금액 계산 (상품 + 추가요금 - 할인 + 미납금)
        long finalPrice = (long)work.getTotalPrice() + totalUnpaid;

        if(finalPrice <0 || finalPrice > Integer.MAX_VALUE){
            throw BillingFatalException.invalidUnpaidAmount(work.getRawData().getUserId(), totalUnpaid, finalPrice);
        }
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
                .settlementMonth(LocalDate.parse(targetMonth)) // 정산월은 배치를 돌리는 해당월.
                .totalPrice((int)finalPrice)
                .settlementDetails(detailsJson)
                .sendStatus(SendStatus.READY)
                .batchExecutionId(jobExecutionId)
                .processedAt(LocalDateTime.now())
                .build();
    }
}
