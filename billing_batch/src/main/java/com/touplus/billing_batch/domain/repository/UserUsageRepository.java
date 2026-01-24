package com.touplus.billing_batch.domain.repository;

import com.touplus.billing_batch.domain.entity.UserUsage;

import java.time.LocalDate;
import java.util.List;

public interface UserUsageRepository {
    List<UserUsage> findByUserIdAndPeriod(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    );
}
