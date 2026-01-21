/*package com.touplus.billing_message.service;

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
/*@Transactional
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
}*/

/**
 * 발송 실패 처리 (외부에서 호출 시)
 */
/*  @Transactional
  public void handleSendFailure(Long messageId, int retryCount, UserBanInfo banInfo) {
      LocalDateTime nextRetry = messagePolicy.nextRetryAt(LocalDateTime.now(), retryCount);
      LocalDateTime adjustedRetry = messagePolicy.adjustForBan(nextRetry, banInfo);
      messageRepository.markFailed(messageId, adjustedRetry);
      log.info("재시도 예약됨 messageId={} at={}", messageId, adjustedRetry);
  }
}
*/

// 미수 : 내용이 아예 달라질 것 같아서 싹 주석 처리하고 내 코드로 새로 만들었음
package com.touplus.billing_message.service;

import com.touplus.billing_message.domain.entity.*;
import com.touplus.billing_message.domain.respository.*;
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

    // JDBC Repository 추가
    private final MessageJdbcRepository messageJdbcRepository;
    private final MessageSnapshotJdbcRepository messageSnapshotJdbcRepository;

    // 스냅샷 이미 다 생성된 후의 발송 처리
    @Transactional
    public void processMessage(Long messageId) {

        Message message = messageRepository.findById(messageId).orElse(null);
        if (message == null) {
            log.warn("메시지 없음 messageId={}", messageId);
            return;
        }

        // SENT는 스킵
        if (message.getStatus() == MessageStatus.SENT) {
            log.debug("이미 SENT 처리됨 messageId={}", messageId);
            return;
        }

        // CREATED만 처리(CREATED가 아니면 넘어감)
        if (message.getStatus() != MessageStatus.CREATED) {
            log.debug("처리 대상 아님 messageId={} status={}",
                    messageId, message.getStatus());
            return;
        }

        log.info("메시지 발송 처리 시작 messageId={} retryCount={}",
                messageId, message.getRetryCount());

        // 발송 타입 결정
        MessageType messageType = message.getRetryCount() >= 3 ? MessageType.SMS : MessageType.EMAIL;

        // snapshot 조회, 없으면 생성
        MessageSnapshot snapshot = messageSnapshotRepository.findById(messageId).orElse(null);

        if (snapshot == null) {
            log.error("Snapshot 없음 messageId={}", messageId);

            LocalDateTime retryAt = messagePolicy.nextRetryAt(LocalDateTime.now(), message.getRetryCount());

            messageRepository.markFailed(messageId, retryAt);
            return;
        }

        // ban 시간대 체크
        UserBanInfo banInfo = userBanRepository.findBanInfo(message.getUserId()).orElse(null);

        LocalDateTime now = LocalDateTime.now();
        if (messagePolicy.isInBanWindow(now, banInfo)) {
            LocalDateTime nextAllowed = messagePolicy.nextAllowedTime(now, banInfo);

            messageRepository.defer(messageId, nextAllowed);
            log.info("금지 시간대 → 연기 messageId={} until={}",
                    messageId, nextAllowed);
            return;
        }

        // 발송
        SendResult result;
        try {
            result = messageSender.send(messageType, snapshot);
        } catch (Exception e) {
            log.error("메시지 발송 예외 messageId={}", messageId, e);

            LocalDateTime retryAt = messagePolicy.adjustForBan(
                    messagePolicy.nextRetryAt(now, message.getRetryCount()),
                    banInfo);

            messageRepository.markFailed(messageId, retryAt);
            return;
        }

        // 로그 (실패해도 무시)
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
            log.warn("SendLog 저장 실패 messageId={}", messageId, e);
        }

        // 결과 반영
        if (result.success()) {
            messageRepository.markSent(messageId);
            log.info("메시지 발송 성공 messageId={} type={}",
                    messageId, messageType);
            return;
        }

        LocalDateTime retryAt = messagePolicy.adjustForBan(
                messagePolicy.nextRetryAt(now, message.getRetryCount()),
                banInfo);

        messageRepository.markFailed(messageId, retryAt);
        log.info("메시지 발송 실패 → 재시도 예약 messageId={} retryAt={}",
                messageId, retryAt);
    }

    // 외부 실패 처리
    @Transactional
    public void handleSendFailure(Long messageId, int retryCount, UserBanInfo banInfo) {
        LocalDateTime retryAt = messagePolicy.adjustForBan(
                messagePolicy.nextRetryAt(LocalDateTime.now(), retryCount),
                banInfo);

        messageRepository.markFailed(messageId, retryAt);
        log.info("외부 실패 처리 messageId={} retryAt={}", messageId, retryAt);
    }

    /**
     * 발송 처리 후 결과 반환 (Bulk UPDATE용)
     * 개별 UPDATE 없이 결과만 반환
     */
    public MessageDispatchService.ProcessResult processMessageAndReturnResult(Long messageId) {
        Message message = messageRepository.findById(messageId).orElse(null);
        if (message == null) {
            log.warn("메시지 없음 messageId={}", messageId);
            return new MessageDispatchService.ProcessResult(messageId, false);
        }

        // SENT는 스킵
        if (message.getStatus() == MessageStatus.SENT) {
            return new MessageDispatchService.ProcessResult(messageId, true);
        }

        // CREATED만 처리
        if (message.getStatus() != MessageStatus.CREATED) {
            return new MessageDispatchService.ProcessResult(messageId, false);
        }

        // 발송 타입 결정
        MessageType messageType = message.getRetryCount() >= 3 ? MessageType.SMS : MessageType.EMAIL;

        // snapshot 조회
        MessageSnapshot snapshot = messageSnapshotRepository.findById(messageId).orElse(null);

        if (snapshot == null) {
            log.error("Snapshot 없음 messageId={}", messageId);
            LocalDateTime retryAt = messagePolicy.nextRetryAt(LocalDateTime.now(), message.getRetryCount());
            messageRepository.markFailed(messageId, retryAt);
            return new MessageDispatchService.ProcessResult(messageId, false);
        }

        // ban 시간대 체크
        UserBanInfo banInfo = userBanRepository.findBanInfo(message.getUserId()).orElse(null);

        LocalDateTime now = LocalDateTime.now();
        if (messagePolicy.isInBanWindow(now, banInfo)) {
            LocalDateTime nextAllowed = messagePolicy.nextAllowedTime(now, banInfo);
            messageRepository.defer(messageId, nextAllowed);
            return new MessageDispatchService.ProcessResult(messageId, false);
        }

        // 발송
        SendResult result;
        try {
            result = messageSender.send(messageType, snapshot);
        } catch (Exception e) {
            log.error("메시지 발송 예외 messageId={}", messageId, e);
            LocalDateTime retryAt = messagePolicy.adjustForBan(
                    messagePolicy.nextRetryAt(now, message.getRetryCount()),
                    banInfo);
            messageRepository.markFailed(messageId, retryAt);
            return new MessageDispatchService.ProcessResult(messageId, false);
        }

        // 로그 저장 (실패해도 무시)
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
            log.warn("SendLog 저장 실패 messageId={}", messageId, e);
        }

        // 결과 반환 (UPDATE는 Bulk로 처리됨)
        if (result.success()) {
            log.debug("메시지 발송 성공 messageId={} type={}", messageId, messageType);
            return new MessageDispatchService.ProcessResult(messageId, true);
        }

        // 실패 시 개별 처리 (재시도 스케줄링 필요)
        LocalDateTime retryAt = messagePolicy.adjustForBan(
                messagePolicy.nextRetryAt(now, message.getRetryCount()),
                banInfo);
        messageRepository.markFailed(messageId, retryAt);
        return new MessageDispatchService.ProcessResult(messageId, false);
    }

    /**
     * JDBC 버전 - 발송 처리 후 결과 반환 (Bulk UPDATE용)
     * JPA Entity 대신 DTO 사용으로 오버헤드 감소
     */
    public MessageDispatchService.ProcessResult processMessageAndReturnResultJdbc(Long messageId) {
        // JDBC로 메시지 조회
        MessageJdbcRepository.MessageDto message = messageJdbcRepository.findById(messageId);
        if (message == null) {
            log.warn("메시지 없음 messageId={}", messageId);
            return new MessageDispatchService.ProcessResult(messageId, false);
        }

        // SENT는 스킵
        if (message.status() == MessageStatus.SENT) {
            return new MessageDispatchService.ProcessResult(messageId, true);
        }

        // CREATED만 처리
        if (message.status() != MessageStatus.CREATED) {
            return new MessageDispatchService.ProcessResult(messageId, false);
        }

        // 발송 타입 결정
        MessageType messageType = message.retryCount() >= 3 ? MessageType.SMS : MessageType.EMAIL;

        // JDBC로 snapshot 조회
        MessageSnapshotJdbcRepository.MessageSnapshotDto snapshotDto = messageSnapshotJdbcRepository
                .findById(messageId);

        if (snapshotDto == null) {
            log.error("Snapshot 없음 messageId={}", messageId);
            LocalDateTime retryAt = messagePolicy.nextRetryAt(LocalDateTime.now(), message.retryCount());
            messageJdbcRepository.markFailed(messageId, retryAt);
            return new MessageDispatchService.ProcessResult(messageId, false);
        }

        // DTO를 Entity로 변환 (발송에 필요)
        MessageSnapshot snapshot = new MessageSnapshot(
                snapshotDto.messageId(),
                snapshotDto.billingId(),
                snapshotDto.settlementMonth(),
                snapshotDto.userId(),
                snapshotDto.userName(),
                snapshotDto.userEmail(),
                snapshotDto.userPhone(),
                snapshotDto.totalPrice(),
                snapshotDto.settlementDetails(),
                snapshotDto.messageContent());

        // ban 시간대 체크
        UserBanInfo banInfo = userBanRepository.findBanInfo(message.userId()).orElse(null);

        LocalDateTime now = LocalDateTime.now();
        if (messagePolicy.isInBanWindow(now, banInfo)) {
            LocalDateTime nextAllowed = messagePolicy.nextAllowedTime(now, banInfo);
            messageJdbcRepository.defer(messageId, nextAllowed);
            return new MessageDispatchService.ProcessResult(messageId, false);
        }

        // 발송
        SendResult result;
        try {
            result = messageSender.send(messageType, snapshot);
        } catch (Exception e) {
            log.error("메시지 발송 예외 messageId={}", messageId, e);
            LocalDateTime retryAt = messagePolicy.adjustForBan(
                    messagePolicy.nextRetryAt(now, message.retryCount()),
                    banInfo);
            messageJdbcRepository.markFailed(messageId, retryAt);
            return new MessageDispatchService.ProcessResult(messageId, false);
        }

        // 로그 저장 (실패해도 무시) - 여전히 JPA 사용
        try {
            messageSendLogRepository.save(
                    new MessageSendLog(
                            messageId,
                            message.retryCount(),
                            messageType,
                            result.code(),
                            result.message(),
                            LocalDateTime.now()));
        } catch (Exception e) {
            log.warn("SendLog 저장 실패 messageId={}", messageId, e);
        }

        // 결과 반환 (UPDATE는 Bulk로 처리됨)
        if (result.success()) {
            log.debug("메시지 발송 성공 (JDBC) messageId={} type={}", messageId, messageType);
            return new MessageDispatchService.ProcessResult(messageId, true);
        }

        // 실패 시 JDBC로 업데이트
        LocalDateTime retryAt = messagePolicy.adjustForBan(
                messagePolicy.nextRetryAt(now, message.retryCount()),
                banInfo);
        messageJdbcRepository.markFailed(messageId, retryAt);
        return new MessageDispatchService.ProcessResult(messageId, false);
    }
}
