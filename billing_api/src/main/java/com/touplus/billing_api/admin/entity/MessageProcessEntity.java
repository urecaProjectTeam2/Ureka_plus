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
public class MessageProcessEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long messageProcessId;

    @Enumerated(EnumType.STRING)
    private ProcessType kafkaReceive;

    @Enumerated(EnumType.STRING)
    private ProcessType createMessage;

    @Enumerated(EnumType.STRING)
    private ProcessType sentMessage;
}
