package com.touplus.billing_api.domain.repository.message;

import com.touplus.billing_api.domain.message.entity.BillingSnapshot;

import java.time.LocalDate;
import java.util.List;

public interface BillingSnapshotRepository {

    // 1. settlement_month 기준 전체 스냅샷 조회
    List<BillingSnapshot> findBySettlementMonth(LocalDate settlementMonth);

    // 2. settlement_month 기준 user_id 목록만 조회 (가장 많이 쓸 것)
    List<Long> findUserIdsBySettlementMonth(LocalDate settlementMonth);
}