package com.touplus.billing_api.admin.repository;

import java.time.LocalDate;

import com.touplus.billing_api.admin.enums.ProcessType;

public interface MessageProcessRepository {

    ProcessType findLatestCreateMessageStatus();
    ProcessType findLatestSentMessageStatus();

    long countCreateMessage(LocalDate settlementMonth);

    long countSentMessage(LocalDate settlementMonth);

    long countTotal();

}
