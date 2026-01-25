package com.touplus.billing_message.scheduler;

import com.touplus.billing_message.domain.entity.Message;
import com.touplus.billing_message.domain.entity.MessageStatus;
import com.touplus.billing_message.domain.respository.MessageRepository;
import com.touplus.billing_message.service.WaitingQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 애플리케이션 시작 시 DB의 WAITED 상태 메시지를 Redis Queue로 동기화
 *
 * 용도: Redis 유실 또는 서버 재시작 시 기존 WAITED 메시지 복구
 * 주의: 앱 시작 시점에 메시지가 없으면 0건 (정상)
 *       새 메시지는 MessageProcessor에서 INSERT 후 바로 Redis에 추가됨
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisQueueInitializer implements ApplicationRunner {

    private final MessageRepository messageRepository;
    private final WaitingQueueService waitingQueueService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("[Redis Init] DB → Redis 동기화 시작...");

        // Redis 큐 초기화 후 DB 기준 재적재
        waitingQueueService.clearQueue();

        // WAITED 상태 메시지 조회
        List<Message> waitedMessages = messageRepository.findByStatus(MessageStatus.WAITED);

        if (waitedMessages.isEmpty()) {
            log.info("[Redis Init] WAITED 상태 메시지 없음 (정상 - 새 메시지는 MessageProcessor에서 추가됨)");
            return;
        }

        log.info("[Redis Init] WAITED 메시지 {}건 발견, Redis 큐에 추가 중...", waitedMessages.size());

        int count = 0;
        for (Message message : waitedMessages) {
            LocalDateTime scheduledAt = message.getScheduledAt() != null
                    ? message.getScheduledAt()
                    : LocalDateTime.now();

            waitingQueueService.addToQueue(message.getMessageId(), scheduledAt);
            count++;
        }

        log.info("[Redis Init] 완료: {}건 Redis 큐에 추가됨", count);
    }
}
