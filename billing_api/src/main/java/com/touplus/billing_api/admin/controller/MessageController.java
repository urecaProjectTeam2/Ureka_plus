package com.touplus.billing_api.admin.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.touplus.billing_api.admin.dto.MessageWithSettlementMonthDto;
import com.touplus.billing_api.domain.message.dto.PageResult;
import com.touplus.billing_api.domain.message.service.MessageService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/message")
public class MessageController {

    private final MessageService messageService;

    /**
     * 전체 메시지 조회 (페이지네이션 지원)
     * GET /admin/message/all?page=0
     */
    @GetMapping("/all")
    public PageResult<MessageWithSettlementMonthDto> getAllMessages(
            @RequestParam(value = "page", defaultValue = "0") int page
    ) {
        return messageService.getMessagesWithPagination(null, null, page);
    }

    /**
     * settlement_month 기준 메시지 조회
     * GET /admin/message?month=202601&page=0
     */
    @GetMapping
    public PageResult<MessageWithSettlementMonthDto> getMessagesByMonth(
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
    public PageResult<MessageWithSettlementMonthDto> getMessagesByStatus(
            @RequestParam("status") String messageStatus,
            @RequestParam(value = "page", defaultValue = "0") int page
    ) {
        return messageService.getMessagesByStatus(messageStatus, page);
    }
    
    
    ///////////////////////// 위의 내용 다 합친 거
    
    /**
     * 동적 조회: status, month 조건 + 최신순 + 페이징
     * GET /admin/message/query?status=SENT&month=202601&page=0
     */
    @GetMapping("/query")
    public PageResult<MessageWithSettlementMonthDto> queryMessages(
            @RequestParam(value = "status", required = false) String messageStatus,
            @RequestParam(value = "month", required = false) String settlementMonth,
            @RequestParam(value = "page", defaultValue = "0") int page
    ) {
        return messageService.getMessagesWithPagination(messageStatus, settlementMonth, page);
    }
}