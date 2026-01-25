package com.touplus.billing_batch.domain.repository.service;

import com.touplus.billing_batch.domain.entity.UserUsage;

import java.time.LocalDate;
import java.util.List;

public interface UserUsageRepository {
    List<UserUsage> findByUserIdIn(
            List<Long> userIds,
            LocalDate startDate,
            LocalDate endDate
    );
}
