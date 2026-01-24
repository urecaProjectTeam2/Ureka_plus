package com.touplus.billing_api.admin.repository;

import com.touplus.billing_api.admin.enums.ProcessType;

public interface MessageProcessRepository {

    ProcessType findLatestKafkaReceiveStatus();
    ProcessType findLatestCreateMessageStatus();
    ProcessType findLatestSentMessageStatus();


}
