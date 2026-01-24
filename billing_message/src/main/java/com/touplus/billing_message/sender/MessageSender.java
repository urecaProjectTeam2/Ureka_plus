package com.touplus.billing_message.sender;

import com.touplus.billing_message.domain.entity.MessageSnapshot;
import com.touplus.billing_message.domain.entity.MessageType;
import java.util.concurrent.CompletableFuture;

public interface MessageSender {

    /**
     * 동기 발송 (하위 호환용)
     */
    SendResult send(MessageType type, MessageSnapshot snapshot);

    /**
     * 비동기 발송 (권장)
     * ScheduledExecutorService를 사용하여 논블로킹 방식으로 딜레이 처리
     */
    default CompletableFuture<SendResult> sendAsync(MessageType type, MessageSnapshot snapshot) {
        // 기본 구현: 동기 호출을 래핑 (구현체에서 오버라이드 권장)
        return CompletableFuture.completedFuture(send(type, snapshot));
    }
}
