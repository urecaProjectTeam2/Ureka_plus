package com.touplus.billing_message.processor;

import com.touplus.billing_message.domain.entity.BillingSnapshot;
import com.touplus.billing_message.domain.entity.Message;
import com.touplus.billing_message.domain.entity.User;
import com.touplus.billing_message.domain.respository.MessageRepository;
import com.touplus.billing_message.domain.respository.MessageJdbcRepository;
import com.touplus.billing_message.domain.respository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * MessageProcessor
 * billing_snapshot 저장 후 호출되어 Message 생성
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MessageProcessor {

    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final MessageJdbcRepository messageJdbcRepository;

    private static final int MAX_RETRY = 3;

    /**
     * BillingSnapshot 목록을 받아 일괄 처리 (Batch)
     * - INSERT IGNORE로 중복 자동 무시
     * - COUNT 비교로 누락 감지, 누락 시에만 상세 검사
     */
    @Transactional
    public void processBatch(java.util.List<BillingSnapshot> snapshots) {
        processBatchWithRetry(snapshots, 0);
    }

    private void processBatchWithRetry(java.util.List<BillingSnapshot> snapshots, int retryCount) {
        if (snapshots.isEmpty()) return;

        String retryLabel = retryCount > 0 ? String.format(" (retry %d번)", retryCount) : "";

        // 1. COUNT 전
        long beforeCount = messageRepository.count();

        // 2. 유저 정보 일괄 조회
        java.util.List<Long> userIds = snapshots.stream()
            .map(BillingSnapshot::getUserId)
            .distinct()
            .toList();
        
        java.util.Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
            .collect(java.util.stream.Collectors.toMap(User::getUserId, u -> u));

        // 3. Message 생성
        java.util.List<Message> messages = new java.util.ArrayList<>();
        
        for (BillingSnapshot snapshot : snapshots) {
            User user = userMap.get(snapshot.getUserId());
            if (user == null) {
                log.warn("유저 정보를 찾을 수 없어 건너뜁니다{}: userId={}", retryLabel, snapshot.getUserId());
                continue;
            }

            LocalDateTime scheduledAt = calculateScheduledTime(user);
            messages.add(new Message(
                snapshot.getBillingId(),
                snapshot.getUserId(),
                scheduledAt,
                user.getBanEndTime()
            ));
        }

        // 4. Batch Insert (INSERT IGNORE - 중복 자동 무시)
        if (!messages.isEmpty()) {
            messageJdbcRepository.batchInsert(messages);
            log.info("Message 배치 저장 완료{}: count={}", retryLabel, messages.size());
        }

        // 5. COUNT 비교로 누락 확인
        long afterCount = messageRepository.count();
        long expectedCount = beforeCount + messages.size();
        
        if (afterCount == expectedCount) {
            // YES: 정상 완료
            return;
        }
        
        // NO: 누락 발생 - 상세 검사 및 재시도
        log.warn("⚠️ COUNT 불일치{}: expected={} actual={}", retryLabel, expectedCount, afterCount);
        
        if (retryCount >= MAX_RETRY) {
            log.error("❌ 최종 실패! (최대 재시도 초과)");
            return;
        }
        
        // 누락된 것만 찾아서 재시도
        java.util.List<Long> billingIds = snapshots.stream()
            .map(BillingSnapshot::getBillingId)
            .toList();
        
        java.util.Set<Long> savedBillingIds = messageRepository.findExistingBillingIds(billingIds);
        
        java.util.List<BillingSnapshot> missing = snapshots.stream()
            .filter(s -> !savedBillingIds.contains(s.getBillingId()))
            .toList();

        if (!missing.isEmpty()) {
            log.warn("⚠️ 누락 {}건 재시도{}", missing.size(), retryLabel);
            processBatchWithRetry(missing, retryCount + 1);
        }
    }

    /**
     * BillingSnapshot을 받아 Message를 생성하고 저장 (단건 처리)
     */
    @Transactional
    public void process(BillingSnapshot snapshot) {
        // 0. 중복 체크
        if (messageRepository.existsByBillingId(snapshot.getBillingId())) {
            log.debug("이미 존재하는 Message: billingId={}", snapshot.getBillingId());
            return;
        }

        // 1. 유저 정보 조회
        User user = userRepository.findById(snapshot.getUserId())
            .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다: " + snapshot.getUserId()));

        // 2. 발송 예정 시간 계산
        LocalDateTime scheduledAt = calculateScheduledTime(user);

        // 3. Message 저장
        Message message = new Message(
            snapshot.getBillingId(),
            snapshot.getUserId(),
            scheduledAt,
            user.getBanEndTime()  // User에서 banEndTime 복사
        );

        messageRepository.save(message);
    }

    /**
     * 발송 예정 시간 계산
     * - sendingDay: 발송 예정일 (1~28)
     * - banStartTime/banEndTime: 발송 금지 시간대
     */
    private LocalDateTime calculateScheduledTime(User user) {
        LocalDate today = LocalDate.now();
        int sendingDay = user.getSendingDay();

        // 1. 날짜 결정
        LocalDate sendDate;
        if (today.getDayOfMonth() < sendingDay) {
            sendDate = today.withDayOfMonth(sendingDay);
        } else {
            sendDate = today.plusMonths(1).withDayOfMonth(sendingDay);
        }

        // 2. 시간 결정 (ban 시간 없으면 09:00)
        LocalTime sendTime = LocalTime.of(9, 0);
        if (user.getBanEndTime() != null) {
            sendTime = user.getBanEndTime().plusMinutes(1);
        }

        return LocalDateTime.of(sendDate, sendTime);
    }
}
