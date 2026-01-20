package com.touplus.billing_message.domain.respository;

import com.touplus.billing_message.domain.entity.MessageTemplate;
import com.touplus.billing_message.domain.entity.MessageType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageTemplateRepository extends JpaRepository<MessageTemplate, Long> {

    Optional<MessageTemplate> findByMessageType(MessageType messageType);
}
