package com.touplus.billing_batch.jobs.billing.step.listener;

import com.touplus.billing_batch.common.BillingException;
import com.touplus.billing_batch.domain.entity.BillingUser;
import com.touplus.billing_batch.domain.dto.BillingCalculationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.SkipListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BillingSkipListener implements SkipListener<BillingUser, BillingCalculationResult> {

    @Override
    public void onSkipInRead(Throwable t) {
        // DB에서 정산 대상자(BillingUser)를 읽어올 때 발생한 에러
        log.error(">> [Skip In Read] Cause: {}", t.getMessage());
    }

    @Override
    public void onSkipInProcess(BillingUser item, Throwable t) {
        // BillingItemProcessor에서 금액 계산 로직 수행 중 발생한 에러
        if (t instanceof BillingException) {
            BillingException be = (BillingException) t;
            log.error(">> [Skip In Process] 비즈니스 에러 - UserID: {}, Code: {}, Message: {}",
                    be.getUserId(), be.getErrorCode(), be.getMessage());
        } else {
            log.error(">> [Skip In Process] 일반 에러 - UserID: {}, Message: {}",
                    item.getUserId(), t.getMessage());
        }
    }

    @Override
    public void onSkipInWrite(BillingCalculationResult item, Throwable t) {
        // BillingItemWriter에서 최종 정산 결과를 DB에 저장할 때 발생한 에러
        // (예: 중복 데이터 삽입, JSON 변환 오류 등)
        log.error(">> [Skip In Write] UserID: {}, TotalPrice: {}, Reason: {}",
                item.getUserId(), item.getTotalPrice(), t.getMessage());
    }
}