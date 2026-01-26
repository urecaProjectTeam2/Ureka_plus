package com.touplus.billing_message.processor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.touplus.billing_message.domain.entity.BillingSnapshot;
import com.touplus.billing_message.domain.entity.Message;
import com.touplus.billing_message.domain.entity.MessageType;
import com.touplus.billing_message.domain.entity.User;
import com.touplus.billing_message.domain.respository.MessageJdbcRepository;
import com.touplus.billing_message.domain.respository.MessageRepository;
import com.touplus.billing_message.domain.respository.UserRepository;
import com.touplus.billing_message.service.MessageSnapshotService;
import com.touplus.billing_message.service.WaitingQueueService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * MessageProcessor
 * billing_snapshot 저장 후 호출되어 Message 생성
 * - Message INSERT → 스냅샷 생성 → Redis 큐에 추가
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MessageProcessor {

    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final MessageJdbcRepository messageJdbcRepository;
    private final MessageSnapshotService messageSnapshotService;
    private final WaitingQueueService waitingQueueService;

    private static final int MAX_RETRY = 3;

    /**
     * 외부에서 User Map을 전달받아 처리 (DB 조회 제거로 성능 향상)
     */
    public void processBatchWithUsers(List<BillingSnapshot> snapshots, Map<Long, User> userMap) {
        // billingMap 생성해서 최적화 버전 호출
        Map<Long, BillingSnapshot> billingMap = snapshots.stream()
                .collect(Collectors.toMap(BillingSnapshot::getBillingId, s -> s));
        processBatchWithMaps(snapshots, userMap, billingMap);
    }

    /**
     * 외부에서 User Map + BillingSnapshot Map을 전달받아 처리 (최적화 버전)
     * - 중복 DB 조회 완전 제거
     * - JDBC batchInsert + Redis Pipeline
     */
    public void processBatchWithMaps(
            List<BillingSnapshot> snapshots,
            Map<Long, User> userMap,
            Map<Long, BillingSnapshot> billingMap) {

        if (snapshots.isEmpty())
            return;

        LocalDate today = LocalDate.now();

        // Message 생성
        List<Message> messages = new ArrayList<>(snapshots.size());

        for (BillingSnapshot snapshot : snapshots) {
            User user = userMap.get(snapshot.getUserId());
            if (user == null)
                continue;

            messages.add(new Message(
                    snapshot.getBillingId(),
                    snapshot.getUserId(),
                    calculateScheduledTime(user, today),
                    user.getBanStartTime(),
                    user.getBanEndTime()));
        }

        if (messages.isEmpty())
            return;

        // 1. Message INSERT
        int inserted = insertBatch(messages);

        if (log.isDebugEnabled()) {
            log.debug("Message insert: expected={}, actual={}", messages.size(), inserted);
        }

        if (inserted > 0) {
            // 2. 스냅샷 생성 + Redis 큐에 추가 (최적화 버전)
            createSnapshotsAndEnqueueOptimized(messages, userMap, billingMap);
            log.info("Message 저장 완료: {}건 → 스냅샷 생성 → Redis 큐에 추가됨", inserted);
        }
    }

    public void processBatch(List<BillingSnapshot> snapshots) {

        if (snapshots.isEmpty())
            return;
        LocalDate today = LocalDate.now();

        int retry = 0;
        List<BillingSnapshot> target = snapshots;

        while (!target.isEmpty() && retry <= MAX_RETRY) {
            retry++;

            // 1. 유저 일괄 조회
            List<Long> userIds = target.stream()
                    .map(BillingSnapshot::getUserId)
                    .distinct()
                    .toList();

            Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                    .collect(Collectors.toMap(User::getUserId, u -> u));

            // 2. Message 생성
            List<Message> messages = new ArrayList<>(target.size());

            for (BillingSnapshot snapshot : target) {
                User user = userMap.get(snapshot.getUserId());
                if (user == null)
                    continue;

                messages.add(new Message(
                        snapshot.getBillingId(),
                        snapshot.getUserId(),
                        calculateScheduledTime(user, today),
                        user.getBanStartTime(),
                        user.getBanEndTime()));
            }

            if (messages.isEmpty())
                return;

            // 3. INSERT (짧은 트랜잭션)
            int inserted = insertBatch(messages);

            if (log.isDebugEnabled()) {
                log.debug("Message insert: expected={}, actual={}", messages.size(), inserted);
            }

            if (inserted == messages.size()) {
                // 스냅샷 생성 + Redis 큐에 추가
                createSnapshotsAndEnqueue(messages);
                log.info("Message 저장 완료: {}건 → 스냅샷 생성 → Redis 큐에 추가됨", inserted);
                return; // 정상 완료
            }

            // 4. 누락만 재시도
            Set<Long> savedBillingIds = messageRepository.findExistingBillingIds(
                    target.stream().map(BillingSnapshot::getBillingId).toList());

            target = target.stream()
                    .filter(s -> !savedBillingIds.contains(s.getBillingId()))
                    .toList();

            log.warn("누락 {}건 재시도 ({}회차)", target.size(), retry);
        }

        if (!target.isEmpty()) {
            log.error("최종 실패 {}건", target.size());
        }
    }

    /**
     * INSERT 후 스냅샷 생성 + Redis 큐에 추가 (레거시 - 기존 호환용)
     */
    private void createSnapshotsAndEnqueue(List<Message> messages) {
        List<Long> billingIds = messages.stream()
                .map(Message::getBillingId)
                .toList();

        // 1. INSERT된 Message 엔티티 조회 (messageId 포함)
        List<Message> insertedMessages = messageRepository.findByBillingIdIn(billingIds);

        if (insertedMessages.isEmpty()) {
            log.warn("INSERT된 메시지 조회 실패");
            return;
        }

        // 2. 스냅샷 생성
        try {
            int snapshotCount = messageSnapshotService.createSnapshotsBatch(insertedMessages, MessageType.EMAIL);
            log.debug("스냅샷 {}건 생성됨", snapshotCount);
        } catch (Exception e) {
            log.error("스냅샷 생성 실패", e);
        }

        // 3. Redis 큐에 Pipeline으로 일괄 추가 (EMAIL 1초 지연)
        Map<Long, LocalDateTime> messageIdScheduledAtMap = new HashMap<>(insertedMessages.size());
        for (Message msg : insertedMessages) {
            messageIdScheduledAtMap.put(msg.getMessageId(), msg.getScheduledAt());
        }
        waitingQueueService.addToQueueBatch(messageIdScheduledAtMap, 1);

        log.debug("Redis Pipeline으로 큐에 {}건 추가됨", insertedMessages.size());
    }

    /**
     * INSERT 후 스냅샷 생성 + Redis 큐에 추가 (최적화 버전)
     * - 중복 DB 조회 제거 (userMap, billingMap 재사용)
     * - JDBC batchInsert 사용
     */
    private void createSnapshotsAndEnqueueOptimized(
            List<Message> messages,
            Map<Long, User> userMap,
            Map<Long, BillingSnapshot> billingMap) {

        List<Long> billingIds = messages.stream()
                .map(Message::getBillingId)
                .toList();

        // 1. INSERT된 Message 엔티티 조회 (messageId 포함)
        List<Message> insertedMessages = messageRepository.findByBillingIdIn(billingIds);

        if (insertedMessages.isEmpty()) {
            log.warn("INSERT된 메시지 조회 실패");
            return;
        }

        // 2. 스냅샷 생성 (최적화 - 중복 조회 제거, JDBC batchInsert)
        try {
            int snapshotCount = messageSnapshotService.createSnapshotsBatchOptimized(
                    insertedMessages, MessageType.EMAIL, userMap, billingMap);
            log.debug("스냅샷 JDBC {}건 생성됨", snapshotCount);
        } catch (Exception e) {
            log.error("스냅샷 생성 실패", e);
        }

        // 3. Redis 큐에 Pipeline으로 일괄 추가 (EMAIL 1초 지연)
        Map<Long, LocalDateTime> messageIdScheduledAtMap = new HashMap<>(insertedMessages.size());
        for (Message msg : insertedMessages) {
            messageIdScheduledAtMap.put(msg.getMessageId(), msg.getScheduledAt());
        }
        waitingQueueService.addToQueueBatch(messageIdScheduledAtMap, 1);

        log.debug("Redis Pipeline으로 큐에 {}건 추가됨", insertedMessages.size());
    }

    @Transactional
    public int insertBatch(List<Message> messages) {
        return messageJdbcRepository.batchInsert(messages);
    }

    /* 발송 예정 시간 계산
        - sendingDay: 발송 예정일 (1~28)
        - banStartTime/banEndTime: 발송 금지 시간대 */
    private LocalDateTime calculateScheduledTime(User user, LocalDate today) {

        int sendingDay = user.getSendingDay();

        // [테스트용] 항상 현재 달의 sendingDay로 생성
        LocalDate sendDate = today.withDayOfMonth(sendingDay);

        // [원본] 날짜가 지났으면 다음 달로 생성
        // LocalDate sendDate = today.getDayOfMonth() < sendingDay
        //         ? today.withDayOfMonth(sendingDay)
        //         : today.plusMonths(1).withDayOfMonth(sendingDay);

        LocalTime sendTime = user.getBanEndTime() != null
                ? user.getBanEndTime().plusMinutes(1)
                : LocalTime.of(9, 0);

        return LocalDateTime.of(sendDate, sendTime);
    }
}
