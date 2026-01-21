package com.touplus.billing_batch.domain.repository;

import com.touplus.billing_batch.domain.entity.UserSubscribeDiscount;

import java.time.LocalDate;
import java.util.List;

public interface UserSubscribeDiscountRepository {

    List<UserSubscribeDiscount> findByUserIdIn(List<Long> userIds, LocalDate startDate, LocalDate endDate);
}
