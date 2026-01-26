package com.touplus.billing_api.admin.service.impl;

import com.touplus.billing_api.admin.dto.BatchProgressSseResponse;
import com.touplus.billing_api.admin.dto.BatchBillingErrorLogDto; // DTO 추가 확인
import com.touplus.billing_api.admin.dto.PartitionStatusDto;
import com.touplus.billing_api.admin.repository.JdbcBatchRepository;
import com.touplus.billing_api.admin.service.BatchMonitoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;


@Slf4j
@Service
@RequiredArgsConstructor
public class BatchMonitoringServiceImpl implements BatchMonitoringService {

    private final JdbcBatchRepository repository;
    @Qualifier("sseTaskExecutor")
    private final ThreadPoolTaskExecutor taskExecutor;

    // TPS 계산을 위한 상태 저장
    private long lastTotalProcessed = 0;
    private long lastTimestamp = System.currentTimeMillis();

    @Override
    public void streamBatchProgress(Long executionId, SseEmitter emitter) {
        taskExecutor.execute(() -> runEnhancedMonitoringLoop(emitter));
    }

    @Override
    public void streamLatestBatchProgress(SseEmitter emitter) {
        taskExecutor.execute(() -> runEnhancedMonitoringLoop(emitter));
    }

    private void runEnhancedMonitoringLoop(SseEmitter emitter) {
        AtomicBoolean emitterClosed = new AtomicBoolean(false);
        log.info("==> [SSE Connection Started] New dashboard client connected.");
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Long currentId = repository.findLatestActiveExecutionId();

                // 1. 배치가 없을 때: 화면을 비활성(Gray-out) 상태로 전송
                if (currentId == null) {
                    try {
                        emitter.send(SseEmitter.event().name("batchUpdate")
                                .data(BatchProgressSseResponse.builder().isCompleted(true).build()));
                    } catch (IOException | IllegalStateException e) {
                        emitterClosed.set(true);
                        return;
                    }
                    Thread.sleep(5000);
                    continue;
                }

                List<PartitionStatusDto> partitions = repository.findPartitionDetails(currentId);
                if (partitions.isEmpty()) { Thread.sleep(2000); continue; }

                // 2. 데이터 집계 및 진행률 계산
                long totalProcessed = partitions.stream().mapToLong(PartitionStatusDto::getWriteCount).sum();
                double progressRate = (totalProcessed / 1000020.0) * 100;
                long totalSkipCount = partitions.stream().mapToLong(PartitionStatusDto::getSkipCount).sum();

                // 3. TPS 및 ETC 계산
                long now = System.currentTimeMillis();
                double timeDiff = (now - lastTimestamp) / 1000.0;
                double tps = (timeDiff > 0) ? (totalProcessed - lastTotalProcessed) / timeDiff : 0;
                long remainingSeconds = (tps > 0) ? (long) ((1000020 - totalProcessed) / tps) : 0;

                lastTotalProcessed = totalProcessed;
                lastTimestamp = now;

                // 4. 에러 로그 조회: 스킵 건수가 있을 때만 조회
                List<BatchBillingErrorLogDto> recentErrors = Collections.emptyList();
                if (totalSkipCount > 0) {
                    recentErrors = repository.findAllErrorLogsByJobId(currentId);
                }

                // 5. 응답 전송: 클릭을 위한 currentJobId와 에러 리스트 포함
                BatchProgressSseResponse response = BatchProgressSseResponse.builder()
                        .currentJobId(currentId) // 프론트엔드 클릭 이벤트용 ID
                        .progress(String.format("%.2f%%", progressRate))
                        .totalProcessed(totalProcessed)
                        .skipCount(totalSkipCount)
                        .tps(Math.round(tps * 10) / 10.0)
                        .etc(formatDuration(remainingSeconds))
                        .partitions(partitions)
                        .recentErrors(recentErrors) // 에러 로그 리스트
                        .isCompleted(false)
                        .build();

                if (emitterClosed.get()) {
                    log.info("Emitter already closed. Stop monitoring loop.");
                    return;
                }

                try {
                    emitter.send(SseEmitter.event().name("batchUpdate").data(response));
                } catch (IOException | IllegalStateException e) {
                    log.info("SSE client disconnected. jobId={}, thread={}",
                            currentId, Thread.currentThread().getName());
                    emitterClosed.set(true);
                    return;
                }


                // 6. 종료 체크
                if (partitions.stream().allMatch(p -> "COMPLETED".equals(p.getStatus()))) {
                    try {
                        emitter.send(SseEmitter.event().name("finished").data("completed"));
                    } catch (Exception ignore) {}
                    break;
                }
                Thread.sleep(2000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Monitoring error", e);
            if (emitterClosed.compareAndSet(false, true)) {
                emitter.completeWithError(e);
            }
        } finally {
            if (emitterClosed.compareAndSet(false, true)) {
                emitter.complete();
                log.info("SSE emitter completed safely.");
            }
        }
    }

    private String formatDuration(long sec) {
        return String.format("%02d:%02d:%02d", sec / 3600, (sec % 3600) / 60, sec % 60);
    }
}