package com.touplus.billing_batch.jobs.billing.step.processor;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.touplus.billing_batch.domain.entity.BillingUser;

@Component
public class BillingItemProcessor
        implements ItemProcessor<BillingUser, BillingUser> {

    @Override
    public BillingUser process(BillingUser user) {
        // DB 조회 금지
        // Writer에서 청크 단위 처리
        return user;
    }
}
