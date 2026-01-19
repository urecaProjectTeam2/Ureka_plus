package com.touplus.billing_batch.jobs.billing.step.processor;

import com.touplus.billing_batch.domain.dto.BillingCalculationResult;
import com.touplus.billing_batch.domain.dto.BillingUserBillingInfoDto;
import org.springframework.batch.item.ItemProcessor;

public class UnpaidAmountProcessor
        implements ItemProcessor<BillingUserBillingInfoDto, BillingCalculationResult> {

    @Override
    public BillingCalculationResult process(BillingUserBillingInfoDto item) throws Exception {
        return null;
    }
}
