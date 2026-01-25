package com.touplus.billing_batch.domain.entity;

import com.touplus.billing_batch.domain.enums.JobType;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchProcess {
    @Id
    @Column(name="batch_process_id", nullable = false)
    Long batchProcessId;

    @Column(name="job")
    JobType job;

    @Column(name ="kafka_sent")
    JobType kafkaSent;

    @Column(name="settlement_month",nullable = false)
    LocalDate settlementMonth;

}
