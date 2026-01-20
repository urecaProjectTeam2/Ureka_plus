package com.touplus.billing_message.domain.respository;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.touplus.billing_message.domain.entity.BillingSnapshot;

public interface BillingSnapshotRepository
        extends JpaRepository<BillingSnapshot, Long> {

	boolean existsByUserIdAndSettlementMonth(Long userId, LocalDate settlementMonth);

	// snapshot 개수 알기 위한 쿼리
    @Query(value = "SELECT COUNT(*) FROM billing_message.billing_snapshot", nativeQuery = true)
    Long countAll();
}
