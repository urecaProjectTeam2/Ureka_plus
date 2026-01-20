package com.touplus.billing_message.domain.respository;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;

import com.touplus.billing_message.domain.entity.BillingSnapshot;

public interface BillingSnapshotRepository
        extends JpaRepository<BillingSnapshot, Long> {

	// 확인해서 없으면 바로 삭제하기 아니면 snapshotDBRepository랑 합치기
	boolean existsByUserIdAndSettlementMonth(Long userId, LocalDate settlementMonth);

}
