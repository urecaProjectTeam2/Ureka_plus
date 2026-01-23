package com.touplus.billing_api.domain.repository.message;

import java.util.List;

import com.touplus.billing_api.admin.dto.MessageWithSettlementMonthDto;

public interface MessagePagingRepository {

	// 전체 조회
    List<MessageWithSettlementMonthDto> findAll(int page, int pageSize);

    // 월별 조회
    List<MessageWithSettlementMonthDto> findBySettlementMonth(String settlementMonth, int page, int pageSize);


    // 상태별 조회
    List<MessageWithSettlementMonthDto> findAllByStatus(String messageStatus, int page, int pageSize);

    long countByStatus(String messageStatus);
    
    // 동적 조회용
    long countMessages(String messageStatus, String settlementMonth);

    List<MessageWithSettlementMonthDto> findMessages(String messageStatus, String settlementMonth, int page, int pageSize);

    
}
