package com.touplus.billing_message.service;

import com.touplus.billing_message.domain.entity.Message;
import com.touplus.billing_message.domain.entity.MessageSendLog;
import com.touplus.billing_message.domain.entity.MessageSnapshot;
import com.touplus.billing_message.domain.entity.MessageType;
import com.touplus.billing_message.domain.respository.MessageRepository;
import com.touplus.billing_message.domain.respository.MessageSendLogRepository;
import com.touplus.billing_message.domain.respository.MessageSnapshotRepository;
import com.touplus.billing_message.domain.respository.UserBanInfo;
import com.touplus.billing_message.domain.respository.UserBanRepository;
import com.touplus.billing_message.sender.MessageSender;
import com.touplus.billing_message.sender.SendResult;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageDispatchService {

    private final MessageRepository messageRepository;
    private final MessageSnapshotRepository messageSnapshotRepository;
    private final MessageSendLogRepository messageSendLogRepository;
    private final UserBanRepository userBanRepository;
    private final MessageSender messageSender;
    private final MessagePolicy messagePolicy;
    private final MessageClaimService messageClaimService;
    private final TaskExecutor messageTaskExecutor;

    public void dispatchDueMessages() {
        List<Long> messageIds = messageClaimService.claimNextMessages(LocalDateTime.now());
        if (messageIds.isEmpty()) {
            log.debug("No due messages found");
            return;
        }

        log.info("Dispatching {} messages", messageIds.size());
        for (Long messageId : messageIds) {
            messageTaskExecutor.execute(() -> processMessage(messageId));
        }
    }

    public void processMessage(Long messageId) {
        Message message = messageRepository.findById(messageId).orElse(null);
        if (message == null) {
            log.warn("Message not found messageId={}", messageId);
            return;
        }

        log.info("Processing messageId={} retryCount={}", messageId, message.getRetryCount());
        MessageSnapshot snapshot = messageSnapshotRepository.findById(messageId).orElse(null);
        if (snapshot == null) {
            log.warn("Missing message_snapshot for messageId={}", messageId);
            LocalDateTime nextRetry = messagePolicy.nextRetryAt(LocalDateTime.now(), message.getRetryCount());
            messageRepository.markFailed(messageId, nextRetry);
            return;
        }

        UserBanInfo banInfo = userBanRepository.findBanInfo(message.getUserId()).orElse(null);
        LocalDateTime now = LocalDateTime.now();
        if (messagePolicy.isInBanWindow(now, banInfo)) {
            LocalDateTime nextAllowed = messagePolicy.nextAllowedTime(now, banInfo);
            messageRepository.defer(messageId, nextAllowed);
            log.info("Deferred messageId={} until {}", messageId, nextAllowed);
            return;
        }

        MessageType messageType = message.getRetryCount() >= 3
                ? MessageType.SMS
                : MessageType.EMAIL;

        SendResult result;
        try {
            result = messageSender.send(messageType, snapshot);
        } catch (Exception e) {
            log.error("Message send failed messageId={}", messageId, e);
            LocalDateTime nextRetry = messagePolicy.nextRetryAt(LocalDateTime.now(), message.getRetryCount());
            LocalDateTime adjustedRetry = messagePolicy.adjustForBan(nextRetry, banInfo);
            messageRepository.markFailed(messageId, adjustedRetry);
            log.info("Retry scheduled messageId={} at {}", messageId, adjustedRetry);
            return;
        }

        messageSendLogRepository.save(
                new MessageSendLog(
                        messageId,
                        message.getRetryCount(),
                        messageType,
                        result.code(),
                        result.message(),
                        LocalDateTime.now()
                )
        );

        if (result.success()) {
            messageRepository.markSent(messageId);
            log.info("Message sent messageId={} type={}", messageId, messageType);
            return;
        }

        LocalDateTime nextRetry = messagePolicy.nextRetryAt(LocalDateTime.now(), message.getRetryCount());
        LocalDateTime adjustedRetry = messagePolicy.adjustForBan(nextRetry, banInfo);
        messageRepository.markFailed(messageId, adjustedRetry);
        log.info("Message failed messageId={} type={} retryAt={}", messageId, messageType, adjustedRetry);
    }
}
