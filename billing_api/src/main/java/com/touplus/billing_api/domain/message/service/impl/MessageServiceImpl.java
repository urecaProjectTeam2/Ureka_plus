package com.touplus.billing_api.domain.message.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.touplus.billing_api.admin.dto.MessageWithSettlementMonthDto;
import com.touplus.billing_api.domain.message.dto.PageResult;
import com.touplus.billing_api.domain.message.service.MessageService;
import com.touplus.billing_api.domain.repository.message.MessagePagingRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessagePagingRepository messagePagingRepository;

    private final int PAGE_SIZE = 20;

    @Override
    public List<MessageWithSettlementMonthDto> getAllMessages(int page) {
        return messagePagingRepository.findAll(page, PAGE_SIZE);
    }

    @Override
    public List<MessageWithSettlementMonthDto> getMessagesBySettlementMonth(String settlementMonth, int page) {
        return messagePagingRepository.findBySettlementMonth(settlementMonth, page, PAGE_SIZE);
    }

    @Override
    public PageResult<MessageWithSettlementMonthDto> getMessagesByStatus(String messageStatus, int page) {
        long totalElements = messagePagingRepository.countByStatus(messageStatus);
        int totalPages = (int) Math.ceil((double) totalElements / PAGE_SIZE);
        List<MessageWithSettlementMonthDto> content = messagePagingRepository.findAllByStatus(messageStatus, page, PAGE_SIZE);

        return new PageResult<>(content, totalPages, totalElements, page);
    }

    @Override
    public PageResult<MessageWithSettlementMonthDto> getMessagesWithPagination(String messageStatus, String settlementMonth, int page) {
        long totalElements = messagePagingRepository.countMessages(messageStatus, settlementMonth);
        int totalPages = (int) Math.ceil((double) totalElements / PAGE_SIZE);
        List<MessageWithSettlementMonthDto> content = messagePagingRepository.findMessages(messageStatus, settlementMonth, page, PAGE_SIZE);
        return new PageResult<>(content, totalPages, totalElements, page);
    }
}