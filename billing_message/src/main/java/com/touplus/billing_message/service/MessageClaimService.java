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

    @Value("${message.dispatch.batch-size:500}")
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

    /**
     * 스케줄 무시하고 WAITED 상태의 메시지 선점 (테스트용)
     */
    @Transactional
    public List<Long> claimNextMessagesIgnoreSchedule() {
        log.info("메시지 조회 (스케줄 무시)");
        List<Long> messageIds = messageRepository.lockNextMessageIdsIgnoreSchedule(batchSize);
        log.info("조회된 메시지 IDs: {}", messageIds);
        if (messageIds.isEmpty()) {
            return messageIds;
        }

        // 상태를 CREATED로 변경
        messageRepository.markCreatedByIds(messageIds);
        log.debug("메시지 {}건 선점 완료, CREATED로 변경", messageIds.size());
        return messageIds;
    }
}
