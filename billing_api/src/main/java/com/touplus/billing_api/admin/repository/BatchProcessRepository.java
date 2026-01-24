package com.touplus.billing_api.admin.repository;

import com.touplus.billing_api.admin.enums.ProcessType;

public interface BatchProcessRepository {

    ProcessType findLatestJobStatus();
    ProcessType findLatestKafkaSentStatus();
}
