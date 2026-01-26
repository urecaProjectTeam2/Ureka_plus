package com.touplus.billing_api.admin.controller;

import com.touplus.billing_api.admin.dto.BatchHistoryDto;
import com.touplus.billing_api.admin.repository.JdbcBatchRepository;
import com.touplus.billing_api.admin.service.BatchMonitoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/admin/batch")
@RequiredArgsConstructor
public class BatchMonitoringController {

    private final JdbcBatchRepository repository;
    private final BatchMonitoringService monitoringService;

    @GetMapping("/history")
    public List<BatchHistoryDto> getHistoryList() {
        // 리포지토리에서 데이터를 가져와 DTO 리스트로 반환
        return repository.findAllHistory();
    }

    @GetMapping(value = "/realtime/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamStatus() {
        SseEmitter emitter = new SseEmitter(0L);
        // 서비스에게 "알아서 최신 거 찾아서 쏴줘"라고 시킴
        monitoringService.streamLatestBatchProgress(emitter);
        return emitter;
    }
}