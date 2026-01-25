package com.touplus.billing_api.domain.repository.message;

import java.util.List;
import java.util.Map;

import com.touplus.billing_api.admin.dto.MessageWithSettlementMonthDto;

public interface MessagePagingRepository {

    // 전체 조회 - 페이징 처리 없음
    List<MessageWithSettlementMonthDto> findAll();
    
	// 전체 조회 - 페이징 있음
    List<MessageWithSettlementMonthDto> findAll(int page, int pageSize);

    // 월별 조회
    List<MessageWithSettlementMonthDto> findBySettlementMonth(String settlementMonth, int page, int pageSize);

    // 상태별 조회
    List<MessageWithSettlementMonthDto> findAllByStatus(String messageStatus, int page, int pageSize);
    
    // 전체 건수
    long countAll();
    
    // 상태 개수 조회
    long countByStatus(String messageStatus);
    
    // 동적 조회용
    long countMessages(String messageStatus, String settlementMonth);

    List<MessageWithSettlementMonthDto> findMessages(String messageStatus, String settlementMonth, int page, int pageSize);

    
    // UI 용
    Map<String, Long> countGroupByStatus();
    
}
