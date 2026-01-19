package com.touplus.billing_message.domain.respository;

import com.touplus.billing_message.domain.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Message Repository
 * 메시지 저장용
 */
public interface MessageRepository extends JpaRepository<Message, Long> {
}
