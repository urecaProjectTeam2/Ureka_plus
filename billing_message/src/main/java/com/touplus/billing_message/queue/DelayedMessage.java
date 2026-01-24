package com.touplus.billing_message.queue;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * DelayQueue에 저장될 메시지 래퍼
 * 1초 후 발송을 위한 지연 시간 관리
 */
public class DelayedMessage implements Delayed {

    private final Long messageId;
    private final long sendAt;  // 발송 시간 (epoch millis)

    public DelayedMessage(Long messageId, long delayMillis) {
        this.messageId = messageId;
        this.sendAt = System.currentTimeMillis() + delayMillis;
    }

    public Long getMessageId() {
        return messageId;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        long diff = sendAt - System.currentTimeMillis();
        return unit.convert(diff, TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed other) {
        if (other instanceof DelayedMessage otherMsg) {
            return Long.compare(this.sendAt, otherMsg.sendAt);
        }
        return Long.compare(this.getDelay(TimeUnit.MILLISECONDS), 
                           other.getDelay(TimeUnit.MILLISECONDS));
    }
}
