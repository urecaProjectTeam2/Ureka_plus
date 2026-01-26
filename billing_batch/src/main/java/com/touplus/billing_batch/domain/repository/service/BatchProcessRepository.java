package com.touplus.billing_batch.domain.repository.service;

import com.touplus.billing_batch.domain.enums.JobType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

public interface BatchProcessRepository {
    int updateJob(JobType job, LocalDate startDate, LocalDate endDate);
    int updateKafkaSent(JobType kafkaSent, LocalDate startDate, LocalDate endDate);

    int insertBatchProcess(LocalDate settlementMonth);
}
