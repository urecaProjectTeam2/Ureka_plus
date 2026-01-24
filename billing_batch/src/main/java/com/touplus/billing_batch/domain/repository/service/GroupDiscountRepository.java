package com.touplus.billing_batch.domain.repository.service;

import com.touplus.billing_batch.domain.entity.GroupDiscount;

import java.util.Optional;

public interface GroupDiscountRepository {

    Optional<GroupDiscount> findByGroupId(Long groupId);
}
