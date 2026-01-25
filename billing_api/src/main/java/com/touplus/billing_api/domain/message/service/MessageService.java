package com.touplus.billing_api.domain.message.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.touplus.billing_api.admin.dto.MessageWithSettlementMonthDto;
import com.touplus.billing_api.admin.dto.PageResponseDto;
import com.touplus.billing_api.domain.message.dto.MessageStatusSummaryDto;
import com.touplus.billing_api.domain.message.enums.MessageStatus;

@Service
public interface MessageService {
	
	List<MessageWithSettlementMonthDto> getAllMessages();

	List<MessageWithSettlementMonthDto> getAllMessages(int page);

    List<MessageWithSettlementMonthDto> getMessagesBySettlementMonth(String settlementMonth, int page);

    PageResponseDto<MessageWithSettlementMonthDto> getMessagesByStatus(MessageStatus messageStatus, int page);

    PageResponseDto<MessageWithSettlementMonthDto> getMessagesWithPagination(MessageStatus messageStatus, String settlementMonth, int page);
}