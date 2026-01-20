package com.touplus.billing_message.scheduler;

import com.touplus.billing_message.service.MessageDispatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageDispatchScheduler {

    private final MessageDispatchService messageDispatchService;

    @Scheduled(fixedDelayString = "${message.dispatch.poll-delay-ms:2000}")
    public void dispatch() {
        messageDispatchService.dispatchDueMessages();
    }
}
