package com.touplus.billing_batch.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.touplus.billing_batch.domain.entity.UserSubscribeProduct;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserSubscribeProductRepository extends JpaRepository<UserSubscribeProduct, Long> {

    @Query("""
        SELECT usp 
        FROM UserSubscribeProduct usp
        WHERE usp.userId = :userId
          AND usp.deletedAt IS NULL
    """)
    List<UserSubscribeProduct> findActiveByUserId(Long userId);

    @Query("""
        SELECT usp
        FROM UserSubscribeProduct usp
        WHERE usp.user.userId IN :userIds
          AND usp.deletedAt IS NULL
    """)
    List<UserSubscribeProduct> findByUserIdIn(@Param("userIds") List<Long> userIds);
}