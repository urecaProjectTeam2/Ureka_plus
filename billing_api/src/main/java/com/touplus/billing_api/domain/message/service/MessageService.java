package com.touplus.billing_api.domain.message.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.touplus.billing_api.admin.dto.MessageWithSettlementMonthDto;
import com.touplus.billing_api.domain.message.dto.PageResult;

@Service
public interface MessageService {
	
	List<MessageWithSettlementMonthDto> getAllMessages(int page);

    List<MessageWithSettlementMonthDto> getMessagesBySettlementMonth(String settlementMonth, int page);

    PageResult<MessageWithSettlementMonthDto> getMessagesByStatus(String messageStatus, int page);

    PageResult<MessageWithSettlementMonthDto> getMessagesWithPagination(String messageStatus, String settlementMonth, int page);
}
