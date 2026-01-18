package com.touplus.billing_message.domain.respository;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;

import com.touplus.billing_message.domain.entity.BillingSnapshot;

public interface BillingSnapshotRepository
        extends JpaRepository<BillingSnapshot, Long> {

	boolean existsByUserIdAndSettlementMonth(Long userId, LocalDate settlementMonth);

}
