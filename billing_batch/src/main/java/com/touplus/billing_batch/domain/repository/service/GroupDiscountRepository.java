package com.touplus.billing_batch.domain.repository.service;

import com.touplus.billing_batch.domain.entity.GroupDiscount;

import java.util.List;
import java.util.Optional;

public interface GroupDiscountRepository {
    List<GroupDiscount> findByUserIdIn(List<Long> groupIds);
}
