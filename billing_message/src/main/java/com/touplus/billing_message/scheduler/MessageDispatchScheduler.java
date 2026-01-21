package com.touplus.billing_message.scheduler;

import com.touplus.billing_message.domain.entity.MessageStatus;
import com.touplus.billing_message.domain.respository.MessageRepository;
import com.touplus.billing_message.service.MessageDispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MessageDispatchScheduler {

    private final MessageDispatchService messageDispatchService;
    private final MessageRepository messageRepository;

    /**
     * 스케줄러 1 - 메인 스케줄러
     * FOR UPDATE SKIP LOCKED로 중복 없이 각자 다른 메시지 배치를 처리
     */
    @Scheduled(fixedDelayString = "${message.dispatch.poll-delay-ms:1000}")
    public void dispatch1() {
        if (!hasWaitingMessages())
            return;
        log.debug("[Scheduler-1] 배치 처리 시작");
        messageDispatchService.dispatchDueMessages();
    }

    @Scheduled(fixedDelayString = "${message.dispatch.poll-delay-ms:1000}", initialDelay = 100)
    public void dispatch2() {
        if (!hasWaitingMessages())
            return;
        log.debug("[Scheduler-2] 배치 처리 시작");
        messageDispatchService.dispatchDueMessages();
    }

    @Scheduled(fixedDelayString = "${message.dispatch.poll-delay-ms:1000}", initialDelay = 200)
    public void dispatch3() {
        if (!hasWaitingMessages())
            return;
        log.debug("[Scheduler-3] 배치 처리 시작");
        messageDispatchService.dispatchDueMessages();
    }

    @Scheduled(fixedDelayString = "${message.dispatch.poll-delay-ms:1000}", initialDelay = 300)
    public void dispatch4() {
        if (!hasWaitingMessages())
            return;
        log.debug("[Scheduler-4] 배치 처리 시작");
        messageDispatchService.dispatchDueMessages();
    }

    @Scheduled(fixedDelayString = "${message.dispatch.poll-delay-ms:1000}", initialDelay = 400)
    public void dispatch5() {
        if (!hasWaitingMessages())
            return;
        log.debug("[Scheduler-5] 배치 처리 시작");
        messageDispatchService.dispatchDueMessages();
    }

    private boolean hasWaitingMessages() {
        return messageRepository.existsByStatus(MessageStatus.WAITED);
    }
}
