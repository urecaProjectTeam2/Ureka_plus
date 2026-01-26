package com.touplus.billing_api.admin.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface BatchMonitoringService {
    void streamBatchProgress(Long executionId, SseEmitter emitter);
    void streamLatestBatchProgress(SseEmitter emitter);
}
