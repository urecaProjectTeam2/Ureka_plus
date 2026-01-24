package com.touplus.billing_batch.domain.repository.service;

import com.touplus.billing_batch.domain.entity.BillingErrorLog;

import java.time.LocalDate;
import java.util.List;

public interface BillingErrorLogRepository {


    //에러 로그 저장
    void save(BillingErrorLog errorLog);


    // 처리되지 않은 에러 조회
    List<BillingErrorLog> findUnprocessed();


    // 특정 정산월 기준 미처리 에러 조회
    List<BillingErrorLog> findUnprocessedBySettlementMonth(LocalDate settlementMonth);


    // 에러 처리 완료 표시
    void markAsProcessed(Long errorLogId);
}
