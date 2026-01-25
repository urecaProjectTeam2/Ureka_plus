package com.touplus.billing_api.admin.controller;

import java.io.IOException;
import java.util.concurrent.Executors;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.touplus.billing_api.domain.message.dto.MessageStatusSummaryDto;
import com.touplus.billing_api.domain.message.service.MessageDashBoardService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/message")
@RequiredArgsConstructor
public class MessageDashBoardController {

    private final MessageDashBoardService messageDashBoardService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/dashboard")
    public String dashboard(org.springframework.ui.Model model, HttpServletRequest request) {
        model.addAttribute("summary", messageDashBoardService.getMessageStatusSummary());
        model.addAttribute("currentPath", request.getRequestURI());
        
        return "message-dashboard";
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamStatus() {

        SseEmitter emitter = new SseEmitter();

        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                while (true) {
                    MessageStatusSummaryDto summary = messageDashBoardService.getMessageStatusSummary();
                    try {
                        emitter.send(summary);
                    } catch (IOException e) {
                        // 클라이언트가 연결을 끊으면 루프 종료
                        break;
                    }
                    Thread.sleep(2000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                emitter.complete();
            }
        });

        return emitter;
    }

}
