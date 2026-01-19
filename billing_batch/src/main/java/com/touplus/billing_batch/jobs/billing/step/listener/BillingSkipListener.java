package com.touplus.billing_batch.jobs.billing.step.listener;

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
        // 읽기 단계에서 에러 발생 시 (예: DB 조회 실패 등)
        log.error(">> [Skip In Read] Cause: {}", t.getMessage());
    }

    @Override
    public void onSkipInProcess(BillingUser item, Throwable t) {
        // 프로세서 단계에서 에러 발생 시 (예: 정산 금액 계산 로직 오류 등)
        // BillingUser 엔티티의 userId를 사용하여 누가 실패했는지 기록
        log.error(">> [Skip In Process] UserID: {}, Reason: {}",
                item.getUserId(), t.getMessage());
    }

    @Override
    public void onSkipInWrite(BillingCalculationResult item, Throwable t) {
        // 쓰기 단계에서 에러 발생 시 (예: 중복 키 제약조건 위반, DB 커넥션, 트랜잭션 실패 등)
        log.error(">> [Skip In Write] UserID: {}, Reason: {}",
                item.getUserId(), t.getMessage());
    }
}