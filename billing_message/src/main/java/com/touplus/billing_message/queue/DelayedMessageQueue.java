package com.touplus.billing_message.queue;

import java.util.concurrent.DelayQueue;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * DelayQueue 래퍼
 * 1초 대기 후 메시지를 방출하는 시간 버퍼 역할
 */
@Component
@Slf4j
public class DelayedMessageQueue {

    private final DelayQueue<DelayedMessage> queue = new DelayQueue<>();

    private static final long DEFAULT_DELAY_MS = 1000; // 1초

    /**
     * 메시지를 큐에 추가 (1초 후 발송 가능)
     */
    public void offer(Long messageId) {
        queue.offer(new DelayedMessage(messageId, DEFAULT_DELAY_MS));
    }

    /**
     * 메시지를 큐에 추가 (지정된 지연 시간)
     */
    public void offer(Long messageId, long delayMillis) {
        queue.offer(new DelayedMessage(messageId, delayMillis));
    }

    /**
     * 시간이 된 메시지를 가져옴 (블로킹)
     * 시간이 안 된 메시지는 대기
     */
    public DelayedMessage take() throws InterruptedException {
        return queue.take();
    }

    /**
     * 시간이 된 메시지를 가져옴 (논블로킹)
     * 없으면 null 반환
     */
    public DelayedMessage poll() {
        return queue.poll();
    }

    /**
     * 현재 큐 크기
     */
    public int size() {
        return queue.size();
    }

    /**
     * 큐가 비어있는지 확인
     */
    public boolean isEmpty() {
        return queue.isEmpty();
    }
}
