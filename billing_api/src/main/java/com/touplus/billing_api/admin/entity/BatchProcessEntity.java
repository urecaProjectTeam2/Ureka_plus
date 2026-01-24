package com.touplus.billing_api.admin.entity;

import com.touplus.billing_api.admin.enums.ProcessType;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class BatchProcessEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long batchProcessId;

    @Enumerated(EnumType.STRING)
    private ProcessType job;

    @Enumerated(EnumType.STRING)
    private ProcessType kafkaSent;
}