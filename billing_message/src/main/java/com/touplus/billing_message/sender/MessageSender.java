package com.touplus.billing_message.sender;

import com.touplus.billing_message.domain.entity.MessageSnapshot;
import com.touplus.billing_message.domain.entity.MessageType;

public interface MessageSender {
    SendResult send(MessageType type, MessageSnapshot snapshot);
}
