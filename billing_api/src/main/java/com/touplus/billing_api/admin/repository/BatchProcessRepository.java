package com.touplus.billing_api.admin.repository;

import java.time.LocalDate;

import com.touplus.billing_api.admin.enums.ProcessType;

public interface BatchProcessRepository {

    ProcessType findLatestJobStatus();
    ProcessType findLatestKafkaSentStatus();
    
    long countBatch(LocalDate settlementMonth);

    long countKafkaSent(LocalDate settlementMonth);
}
