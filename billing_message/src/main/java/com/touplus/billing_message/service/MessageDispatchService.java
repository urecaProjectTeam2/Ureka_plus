package com.touplus.billing_message.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.touplus.billing_message.domain.entity.Message;
import com.touplus.billing_message.domain.entity.MessageType;
import com.touplus.billing_message.queue.DelayedMessageQueue;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 메시지 디스패치 서비스 (Reader 역할)
 * DB에서 메시지 조회 → 스냅샷 생성 → DelayQueue에 투입
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageDispatchService {

    private final MessageClaimService messageClaimService;
    private final MessageSnapshotService messageSnapshotService;
    private final DelayedMessageQueue delayedMessageQueue;

    /**
     * 메시지 조회/스냅샷 생성 후 DelayQueue에 투입
     * @return 투입된 메시지 수
     */
    @Transactional
    public int prepareAndEnqueue(LocalDateTime now) {
        // 1. Claim: WAITED → CREATED
        List<Message> messages = messageClaimService.claimNextMessagesAsEntities(now);

        if (messages.isEmpty()) {
            return 0;
        }

        // 2. 스냅샷 생성
        messageSnapshotService.createSnapshotsBatch(messages, MessageType.EMAIL);

        // 3. DelayQueue에 투입 (1초 후 발송 가능)
        for (Message message : messages) {
            delayedMessageQueue.offer(message.getMessageId());
        }

        log.info("DelayQueue 투입: {}건", messages.size());
        return messages.size();
    }

    /**
     * 발송 대상 메시지를 조회하여 DelayQueue에 투입 (스케줄러에서 호출)
     */
    public void dispatchDueMessages() {
        int enqueued = prepareAndEnqueue(LocalDateTime.now());

        if (enqueued == 0) {
            log.debug("발송 대상 메시지 없음");
        }
    }

    /**
     * 모든 WAITED 상태 메시지를 조회하여 DelayQueue에 투입
     * @return 투입된 메시지 총 건수
     */
    public int dispatchAllWaitedMessages() {
        int totalEnqueued = 0;

        while (true) {
            int enqueued = prepareAndEnqueue(LocalDateTime.now());

            if (enqueued == 0) {
                break;
            }

            totalEnqueued += enqueued;
            log.info("배치 투입 완료: {}건, 누적: {}건", enqueued, totalEnqueued);
        }

        return totalEnqueued;
    }

    /**
     * 현재 DelayQueue 크기 (모니터링용)
     */
    public int getQueueSize() {
        return delayedMessageQueue.size();
    }
}
