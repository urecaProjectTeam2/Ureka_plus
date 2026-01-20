package com.touplus.billing_message.service;

import com.touplus.billing_message.domain.entity.Message;
import com.touplus.billing_message.domain.entity.MessageSendLog;
import com.touplus.billing_message.domain.entity.MessageSnapshot;
import com.touplus.billing_message.domain.entity.MessageStatus;
import com.touplus.billing_message.domain.entity.MessageType;
import com.touplus.billing_message.domain.respository.MessageRepository;
import com.touplus.billing_message.domain.respository.MessageSendLogRepository;
import com.touplus.billing_message.domain.respository.MessageSnapshotRepository;
import com.touplus.billing_message.domain.respository.UserBanInfo;
import com.touplus.billing_message.domain.respository.UserBanRepository;
import com.touplus.billing_message.sender.MessageSender;
import com.touplus.billing_message.sender.SendResult;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageProcessService {

    private final MessageRepository messageRepository;
    private final MessageSnapshotRepository messageSnapshotRepository;
    private final MessageSendLogRepository messageSendLogRepository;
    private final UserBanRepository userBanRepository;
    private final MessageSender messageSender;
    private final MessagePolicy messagePolicy;
    private final MessageSnapshotService messageSnapshotService;

    /**
     * 메시지 처리 (단일 트랜잭션)
     */
    @Transactional
    public void processMessage(Long messageId) {
        Message message = messageRepository.findById(messageId).orElse(null);
        if (message == null) {
            log.warn("메시지를 찾을 수 없음 messageId={}", messageId);
            return;
        }

        // 이미 처리된 메시지는 스킵
        if (message.getStatus() == MessageStatus.SENT) {
            log.debug("이미 발송 완료된 메시지 스킵 messageId={}", messageId);
            return;
        }

        // CREATED 상태가 아니면 스킵 (다른 스레드가 처리 중이거나 상태 불일치)
        if (message.getStatus() != MessageStatus.CREATED) {
            log.debug("처리 대상 상태가 아님 messageId={} status={}", messageId, message.getStatus());
            return;
        }

        log.info("메시지 처리 시작 messageId={} retryCount={}", messageId, message.getRetryCount());

        // 발송 타입 결정 (retryCount >= 3이면 SMS, 아니면 EMAIL)
        MessageType messageType = message.getRetryCount() >= 3
                ? MessageType.SMS
                : MessageType.EMAIL;

        log.info("발송 타입 결정 messageType={} messageId={}", messageType, messageId);

        // snapshot 조회, 없으면 생성
        MessageSnapshot snapshot = messageSnapshotRepository.findById(messageId).orElse(null);
        if (snapshot == null) {
            log.info("스냅샷 생성 중 messageId={} messageType={}", messageId, messageType);
            snapshot = messageSnapshotService.createSnapshot(message, messageType).orElse(null);
            if (snapshot == null) {
                log.warn("스냅샷 생성 실패 messageId={}", messageId);
                LocalDateTime nextRetry = messagePolicy.nextRetryAt(LocalDateTime.now(), message.getRetryCount());
                messageRepository.markFailed(messageId, nextRetry);
                return;
            }
        }

        // ban 시간대 체크
        UserBanInfo banInfo = userBanRepository.findBanInfo(message.getUserId()).orElse(null);
        LocalDateTime now = LocalDateTime.now();
        if (messagePolicy.isInBanWindow(now, banInfo)) {
            LocalDateTime nextAllowed = messagePolicy.nextAllowedTime(now, banInfo);
            messageRepository.defer(messageId, nextAllowed);
            log.info("금지 시간대로 연기됨 messageId={} until={}", messageId, nextAllowed);
            return;
        }

        // 발송
        SendResult result;
        try {
            result = messageSender.send(messageType, snapshot);
        } catch (Exception e) {
            log.error("메시지 발송 실패 messageId={}", messageId, e);
            LocalDateTime nextRetry = messagePolicy.nextRetryAt(LocalDateTime.now(), message.getRetryCount());
            LocalDateTime adjustedRetry = messagePolicy.adjustForBan(nextRetry, banInfo);
            messageRepository.markFailed(messageId, adjustedRetry);
            log.info("재시도 예약됨 messageId={} at={}", messageId, adjustedRetry);
            return;
        }

        // 로그 저장 (실패해도 상태 업데이트에 영향 없음)
        try {
            messageSendLogRepository.save(
                    new MessageSendLog(
                            messageId,
                            message.getRetryCount(),
                            messageType,
                            result.code(),
                            result.message(),
                            LocalDateTime.now()));
        } catch (Exception e) {
            log.warn("발송 로그 저장 실패 (무시됨) messageId={} retryCount={}: {}",
                    messageId, message.getRetryCount(), e.getMessage());
        }

        // 결과 처리
        if (result.success()) {
            messageRepository.markSent(messageId);
            log.info("메시지 발송 완료 messageId={} type={}", messageId, messageType);
            return;
        }

        LocalDateTime nextRetry = messagePolicy.nextRetryAt(LocalDateTime.now(), message.getRetryCount());
        LocalDateTime adjustedRetry = messagePolicy.adjustForBan(nextRetry, banInfo);
        messageRepository.markFailed(messageId, adjustedRetry);
        log.info("메시지 발송 실패, 재시도 예약 messageId={} type={} retryAt={}", messageId, messageType, adjustedRetry);
    }

    /**
     * 발송 실패 처리 (외부에서 호출 시)
     */
    @Transactional
    public void handleSendFailure(Long messageId, int retryCount, UserBanInfo banInfo) {
        LocalDateTime nextRetry = messagePolicy.nextRetryAt(LocalDateTime.now(), retryCount);
        LocalDateTime adjustedRetry = messagePolicy.adjustForBan(nextRetry, banInfo);
        messageRepository.markFailed(messageId, adjustedRetry);
        log.info("재시도 예약됨 messageId={} at={}", messageId, adjustedRetry);
    }
}
