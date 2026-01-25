package com.touplus.billing_batch.jobs.billing.step.listener;

import com.touplus.billing_batch.domain.dto.BillingUserBillingInfoDto;
import com.touplus.billing_batch.domain.entity.BillingResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@StepScope
@Component
@RequiredArgsConstructor
public class BillingSkipListener implements SkipListener<BillingUserBillingInfoDto, BillingResult> {

    private final BillingErrorLogger billingErrorLogger;

    @Value("#{stepExecution}")
    private StepExecution stepExecution;

    @Override
    public void onSkipInRead(Throwable t) {
        log.error(">> [Skip In Read] Cause: {}", t.getMessage());
        billingErrorLogger.saveErrorLog(stepExecution, 0L, t, "READER", false);
        // Read 단계는 대상 유저를 특정하기 어려우므로 필요 시 시스템 에러로 기록
    }

    @Override
    public void onSkipInProcess(BillingUserBillingInfoDto item, Throwable t) {
        Long userId = (item != null) ? item.getUserId() : 0L;
        log.error(">>> [Skip In Process] UserID: [{}], Error: {}", userId, t.getMessage());

        if (item != null) {
            // 1. Users 정보 (DTO에 @ToString 추가 시 상세 내용이 찍힘)
            String userInfo = (item.getUsers() != null) ? item.getUsers().toString() : "NULL";

            // 2. 리스트 사이즈 확인
            int products = (item.getProducts() != null) ? item.getProducts().size() : 0;
            int usages = (item.getUsage() != null) ? item.getUsage().size() : 0;

            log.error("    [Skip Details] Users: {}, Products: {}, Usages: {}", userInfo, products, usages);

            // 3. 사용량 데이터의 실제 월(Month) 확인 (매우 중요)
            // UserUsageDto에도 @ToString이 있으면 객체 내용이 보이고,
            // 없으면 최소한 getUseMonth() 메서드가 있는지 확인하여 호출해야 합니다.
            if (usages > 0) {
                try {
                    // UserUsageDto에 getUseMonth()가 있다고 가정
                    log.error("    [Sample Usage Date] First Usage Month: {}", item.getUsage().get(0).getUseMonth());
                } catch (Exception e) {
                    // 메서드가 없을 경우를 대비한 안전장치
                    log.error("    [Sample Usage Date] {}", item.getUsage().get(0).toString());
                }
            }
        }

        billingErrorLogger.saveErrorLog(stepExecution, userId, t, "PROCESSOR", false);
    }

    @Override
    public void onSkipInWrite(BillingResult item, Throwable t) {
        log.error(">> [Skip In Write] UserID: {}, Reason: {}", item.getUserId(), t.getMessage());
        billingErrorLogger.saveErrorLog(stepExecution, item.getUserId(), t, "WRITER", false);
    }
}