package com.touplus.billing_message.sender;

import com.touplus.billing_message.domain.entity.MessageSnapshot;
import com.touplus.billing_message.domain.entity.MessageType;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

@Component
public class MockMessageSender implements MessageSender {

    @Override
    public SendResult send(MessageType type, MessageSnapshot snapshot) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return SendResult.fail("INTERRUPTED", "send interrupted");
        }

        if (type == MessageType.SMS) {
            return SendResult.ok("OK", "sms sent");
        }

        boolean failed = ThreadLocalRandom.current().nextInt(100) == 0;
        if (failed) {
            return SendResult.fail("MOCK_FAIL", "email send failed");
        }
        return SendResult.ok("OK", "email sent");
    }
}
