package com.touplus.billing_batch.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;
import java.util.List;

public interface BillingResultRepository extends JpaRepository<BillingResult, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select b
        from BillingResult b
        where b.sendStatus = com.touplus.billing_batch.domain.SendStatus.READY
        order by b.id
        """)
    List<BillingResult> findReadyForSend(Pageable pageable);
}
