package com.touplus.billing_message.service;

import com.touplus.billing_message.domain.respository.MessageRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageClaimService {

    private final MessageRepository messageRepository;

    @Value("${message.dispatch.batch-size:40}")
    private int batchSize;

    @Transactional
    public List<Long> claimNextMessages(LocalDateTime now) {
        log.info("Claiming messages at: {}", now);
        List<Long> messageIds = messageRepository.lockNextMessageIds(now, batchSize);
        log.info("Found messageIds: {}", messageIds);
        if (messageIds.isEmpty()) {
            return messageIds;
        }

        // TEMP: use SENT as in-progress marker until SENDING is added.
        messageRepository.markSentByIds(messageIds);
        log.debug("Claimed {} messages", messageIds.size());
        return messageIds;
    }
}
