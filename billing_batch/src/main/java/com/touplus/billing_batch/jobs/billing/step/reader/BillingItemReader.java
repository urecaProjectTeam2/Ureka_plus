package com.touplus.billing_batch.jobs.billing.step.reader;

import java.util.Iterator;

import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;
import com.touplus.billing_batch.domain.entity.BillingUser;
import com.touplus.billing_batch.domain.repository.BillingUserRepository;

@Component
public class BillingItemReader implements ItemReader<BillingUser> {

    private final BillingUserRepository userRepository;
    private Iterator<BillingUser> iterator;

    public BillingItemReader(BillingUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public BillingUser read() {
        if (iterator == null) {
            iterator = userRepository.findAll().iterator();
        }
        return iterator.hasNext() ? iterator.next() : null;
    }
}