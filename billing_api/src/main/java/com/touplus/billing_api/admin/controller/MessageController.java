package com.touplus.billing_api.admin.controller;

import java.util.List;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.touplus.billing_api.admin.dto.MessageWithSettlementMonthDto;
import com.touplus.billing_api.admin.dto.PageResponseDto;
import com.touplus.billing_api.domain.message.enums.MessageStatus;
import com.touplus.billing_api.domain.message.service.MessageService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/message")
public class MessageController {

    private final MessageService messageService;

    /**
     * 전체 메시지 조회 - 페이징 없음
     * GET /admin/message/all
     */
    @GetMapping(value = "/all", params = "!page")
    public List<MessageWithSettlementMonthDto> getAllMessages() {
        return messageService.getAllMessages();
    }

    /**
     * 전체 메시지 조회 (페이지네이션 지원)
     * GET /admin/message/all?page=0
     */
    @GetMapping(value = "/all", params = "page")
    public List<MessageWithSettlementMonthDto> getAllMessages(
            @RequestParam(value = "page", defaultValue = "0") int page
    ) {
        return messageService.getAllMessages(page);
    }
    
    /**
     * settlement_month 기준 메시지 조회
     * GET /admin/message?month=20251201&page=0
     * 달만 빼와서 처리하는 로직 필요할 듯
     */
    @GetMapping
    public PageResponseDto<MessageWithSettlementMonthDto> getMessagesByMonth(
            @RequestParam(value = "month") String settlementMonth,
            @RequestParam(value = "page", defaultValue = "0") int page
    ) {
        return messageService.getMessagesWithPagination(null, settlementMonth, page);
    }

    /**
     * 상태별 메시지 조회
     * GET /admin/message/status?status=SENT&page=0
     */
    @GetMapping("/status")
    public PageResponseDto<MessageWithSettlementMonthDto> getMessagesByStatus(
            @RequestParam("status") String messageStatusStr,
            @RequestParam(value = "page", defaultValue = "0") int page
    ) {
        MessageStatus messageStatus = null;
        if (messageStatusStr != null && !messageStatusStr.isEmpty()) {
            messageStatus = MessageStatus.valueOf(messageStatusStr.toUpperCase());
        }
        return messageService.getMessagesByStatus(messageStatus, page);
    }

    /**
     * 동적 조회: status, month 조건 + 최신순 + 페이징
     * GET /admin/message/query?status=SENT&month=20251201&page=0
     */
    @GetMapping("/query")
    public PageResponseDto<MessageWithSettlementMonthDto> queryMessages(
            @RequestParam(value = "status", required = false) String messageStatusStr,
            @RequestParam(value = "month", required = false) String settlementMonth,
            @RequestParam(value = "page", defaultValue = "0") int page
    ) {
        MessageStatus messageStatus = null;
        if (messageStatusStr != null && !messageStatusStr.isEmpty()) {
            messageStatus = MessageStatus.valueOf(messageStatusStr.toUpperCase());
        }
        return messageService.getMessagesWithPagination(messageStatus, settlementMonth, page);
    }
}