package com.touplus.billing_api.admin.controller;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Executors;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.touplus.billing_api.admin.dto.WholeProcessDto;
import com.touplus.billing_api.admin.entity.BatchProcessEntity;
import com.touplus.billing_api.admin.entity.MessageProcessEntity;
import com.touplus.billing_api.admin.service.WholeProcessService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin/process")
@RequiredArgsConstructor
public class WholeProcessController {

    private final WholeProcessService wholeProcessService;

    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpServletRequest request) {
        model.addAttribute("batch", wholeProcessService.getBatchStatus());
        model.addAttribute("message", wholeProcessService.getMessageStatus());
        model.addAttribute("whole", wholeProcessService.getWholeProcessStatus());
        model.addAttribute("currentPath", request.getRequestURI());
        
        return "whole-process-dashboard";
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamProcessStatus() {

        // 타임아웃 무한
        SseEmitter emitter = new SseEmitter(0L);

        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                while (true) {
                    BatchProcessEntity batch = wholeProcessService.getBatchStatus();
                    MessageProcessEntity message = wholeProcessService.getMessageStatus();
                    WholeProcessDto whole = wholeProcessService.getWholeProcessStatus();

                    Map<String, Object> data = Map.of(
                        "batchJob", batch.getJob(),
                        "batchKafkaSent", batch.getKafkaSent(),
                        "messageKafkaReceive", message.getKafkaReceive(),
                        "messageCreateMessage", message.getCreateMessage(),
                        "messageSentMessage", message.getSentMessage()
                    );

                    try {
                        emitter.send(data);
                        emitter.send(whole);
                    } catch (IOException e) {
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
