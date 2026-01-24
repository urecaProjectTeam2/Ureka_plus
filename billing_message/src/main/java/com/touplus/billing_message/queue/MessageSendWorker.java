package com.touplus.billing_message.queue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.touplus.billing_message.domain.entity.MessageSnapshot;
import com.touplus.billing_message.domain.entity.MessageType;
import com.touplus.billing_message.domain.respository.MessageSnapshotJdbcRepository;
import com.touplus.billing_message.sender.MessageSender;
import com.touplus.billing_message.sender.SendResult;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 메시지 발송 워커
 * DelayQueue에서 메시지를 꺼내 병렬로 발송 처리
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MessageSendWorker {

    private final DelayedMessageQueue delayedMessageQueue;
    private final SendResultBuffer sendResultBuffer;
    private final MessageSnapshotJdbcRepository messageSnapshotJdbcRepository;
    private final MessageSender messageSender;

    @Value("${message.dispatch.worker-count:20}")
    private int workerCount;

    private ExecutorService workerPool;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @PostConstruct
    public void start() {
        workerPool = Executors.newFixedThreadPool(workerCount, r -> {
            Thread t = new Thread(r);
            t.setName("msg-worker-" + t.getId());
            t.setDaemon(true);
            return t;
        });

        running.set(true);

        for (int i = 0; i < workerCount; i++) {
            workerPool.submit(this::workerLoop);
        }

        log.info("MessageSendWorker 시작: {}개 워커", workerCount);
    }

    /**
     * 워커 루프 - DelayQueue에서 메시지를 꺼내 발송
     */
    private void workerLoop() {
        while (running.get()) {
            try {
                // DelayQueue에서 시간 된 메시지 가져오기 (블로킹)
                DelayedMessage delayed = delayedMessageQueue.take();
                Long messageId = delayed.getMessageId();

                // 스냅샷 조회
                MessageSnapshotJdbcRepository.MessageSnapshotDto snapshotDto = 
                    messageSnapshotJdbcRepository.findById(messageId);

                if (snapshotDto == null) {
                    log.warn("스냅샷 없음: messageId={}", messageId);
                    continue;
                }

                // DTO → Entity 변환
                MessageSnapshot snapshot = new MessageSnapshot(
                    snapshotDto.messageId(),
                    snapshotDto.billingId(),
                    snapshotDto.settlementMonth(),
                    snapshotDto.userId(),
                    snapshotDto.userName(),
                    snapshotDto.userEmail(),
                    snapshotDto.userPhone(),
                    snapshotDto.totalPrice(),
                    snapshotDto.settlementDetails(),
                    snapshotDto.messageContent()
                );

                // 발송
                MessageType type = MessageType.EMAIL;
                SendResult result = messageSender.send(type, snapshot);

                // 결과 버퍼에 추가
                if (result.success()) {
                    sendResultBuffer.addSuccess(messageId, type, result.code(), result.message());
                } else {
                    sendResultBuffer.addFailure(messageId, type, result.code(), result.message());
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("워커 처리 중 예외", e);
            }
        }
    }

    @PreDestroy
    public void stop() {
        log.info("MessageSendWorker 종료 시작");
        running.set(false);
        
        if (workerPool != null) {
            workerPool.shutdownNow();
        }
        
        // 남은 결과 플러시
        sendResultBuffer.flush();
        log.info("MessageSendWorker 종료 완료");
    }
}
