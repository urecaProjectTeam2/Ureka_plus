package com.touplus.billing_batch.domain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.touplus.billing_batch.domain.entity.UserSubscribeProduct;

public interface UserSubscribeProductRepository
        extends JpaRepository<UserSubscribeProduct, Long> {

    @Query("""
        SELECT u
        FROM UserSubscribeProduct u
        WHERE u.userId IN :userIds
          AND u.deletedAt IS NULL
    """)
    List<UserSubscribeProduct> findActiveByUserIds(List<Long> userIds);
}
