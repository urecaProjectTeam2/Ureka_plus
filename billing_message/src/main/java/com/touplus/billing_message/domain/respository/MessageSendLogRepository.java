package com.touplus.billing_message.domain.respository;

import com.touplus.billing_message.domain.entity.MessageSendLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageSendLogRepository extends JpaRepository<MessageSendLog, Long> {
}
