package com.touplus.billing_message.event;

import lombok.extern.slf4j.Slf4j;

/**
 * [비활성화] Redis ZSet 기반으로 전환됨
 * - MessageProcessor에서 Redis 큐에 직접 추가
 * - MessageDispatchScheduler에서 Redis 폴링으로 발송 처리
 */
// @Component  // 비활성화
// @RequiredArgsConstructor
@Slf4j
public class MessageEventListener {

    // private final MessageDispatchService messageDispatchService;

    // @Async
    // @EventListener
    // public void handleMessageReadyEvent(MessageReadyEvent event) {
    //     log.info("이벤트 수신: 메시지 {}건 발송 트리거", event.getMessageCount());
    //     messageDispatchService.dispatchAllWaitedMessages();
    // }
}
