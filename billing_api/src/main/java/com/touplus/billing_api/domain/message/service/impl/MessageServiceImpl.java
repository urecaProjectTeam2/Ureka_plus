package com.touplus.billing_api.domain.message.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.touplus.billing_api.admin.dto.MessageWithSettlementMonthDto;
import com.touplus.billing_api.admin.dto.PageResponseDto;
import com.touplus.billing_api.domain.message.dto.MessageStatusSummaryDto;
import com.touplus.billing_api.domain.message.enums.MessageStatus;
import com.touplus.billing_api.domain.message.service.MessageService;
import com.touplus.billing_api.domain.repository.message.MessagePagingRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessagePagingRepository messagePagingRepository;

    private static final int PAGE_SIZE = 20;
    
    @Override
    public List<MessageWithSettlementMonthDto> getAllMessages() {
        return messagePagingRepository.findAll();
    }
    
    @Override
    public List<MessageWithSettlementMonthDto> getAllMessages(int page) {
        return messagePagingRepository.findAll(page, PAGE_SIZE);
    }

    @Override
    public List<MessageWithSettlementMonthDto> getMessagesBySettlementMonth(String settlementMonth, int page) {
        return messagePagingRepository.findBySettlementMonth(settlementMonth, page, PAGE_SIZE);
    }

    @Override
    public PageResponseDto<MessageWithSettlementMonthDto> getMessagesByStatus(
            MessageStatus messageStatus, int page) {

        String status = messageStatus != null ? messageStatus.name() : null;

        long totalElements = messagePagingRepository.countByStatus(status);
        int totalPages = (int) Math.ceil((double) totalElements / PAGE_SIZE);

        List<MessageWithSettlementMonthDto> contents =
                messagePagingRepository.findAllByStatus(status, page, PAGE_SIZE);

        return PageResponseDto.<MessageWithSettlementMonthDto>builder()
                .contents(contents)
                .page(page)
                .size(PAGE_SIZE)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .build();
    }

    @Override
    public PageResponseDto<MessageWithSettlementMonthDto> getMessagesWithPagination(
            MessageStatus messageStatus, String settlementMonth, int page) {

        if (messageStatus == null || settlementMonth == null || settlementMonth.isEmpty()) {

            long totalElements = messagePagingRepository.countAll();
            int totalPages = (int) Math.ceil((double) totalElements / PAGE_SIZE);

            List<MessageWithSettlementMonthDto> contents =
                    messagePagingRepository.findAll(page, PAGE_SIZE);

            return PageResponseDto.<MessageWithSettlementMonthDto>builder()
                    .contents(contents)
                    .page(page)
                    .size(PAGE_SIZE)
                    .totalElements(totalElements)
                    .totalPages(totalPages)
                    .build();
        }
        
        String status = messageStatus.name();

        long totalElements =
                messagePagingRepository.countMessages(status, settlementMonth);
        int totalPages = (int) Math.ceil((double) totalElements / PAGE_SIZE);

        List<MessageWithSettlementMonthDto> contents =
                messagePagingRepository.findMessages(status, settlementMonth, page, PAGE_SIZE);

        return PageResponseDto.<MessageWithSettlementMonthDto>builder()
                .contents(contents)
                .page(page)
                .size(PAGE_SIZE)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .build();
    }
}
