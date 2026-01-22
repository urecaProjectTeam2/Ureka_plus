package com.touplus.billing_message.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.touplus.billing_message.domain.entity.Message;
import com.touplus.billing_message.domain.respository.MessageRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageClaimService {

    private final MessageRepository messageRepository;

    @Value("${message.dispatch.batch-size:1000}")
    private int batchSize;

    @Transactional
    public List<Long> claimNextMessages(LocalDateTime now) {
        log.info("메시지 조회 시작 at={}", now);
        List<Long> messageIds = messageRepository.lockNextMessageIds(now, batchSize);
        log.info("조회된 메시지 IDs: {}", messageIds);
        if (messageIds.isEmpty()) {
            return messageIds;
        }

        // 상태를 CREATED로 변경하여 다른 스레드가 같은 메시지를 처리하지 못하게 함
        messageRepository.markCreatedByIds(messageIds);
        log.debug("메시지 {}건 선점 완료, CREATED로 변경", messageIds.size());
        return messageIds;
    }
    
    @Transactional
    public List<Message> claimNextMessagesAsEntities(LocalDateTime now) {

        List<Message> messages =
                messageRepository.lockNextMessages(now, batchSize);

        if (messages.isEmpty()) {
            return messages;
        }

        List<Long> ids = messages.stream()
                .map(Message::getMessageId)
                .toList();

        messageRepository.markCreatedByIds(ids);

        return messages;
    }

}
