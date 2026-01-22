package com.touplus.billing_message.processor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.touplus.billing_message.domain.entity.BillingSnapshot;
import com.touplus.billing_message.domain.entity.Message;
import com.touplus.billing_message.domain.entity.User;
import com.touplus.billing_message.domain.respository.MessageJdbcRepository;
import com.touplus.billing_message.domain.respository.MessageRepository;
import com.touplus.billing_message.domain.respository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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

    public void processBatch(List<BillingSnapshot> snapshots) {
        if (snapshots.isEmpty()) return;

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
            List<Message> messages = new ArrayList<>();
            for (BillingSnapshot snapshot : target) {
                User user = userMap.get(snapshot.getUserId());
                if (user == null) continue;

                messages.add(new Message(
                        snapshot.getBillingId(),
                        snapshot.getUserId(),
                        calculateScheduledTime(user),
                        user.getBanEndTime()
                ));
            }

            if (messages.isEmpty()) return;

            // 3. INSERT (짧은 트랜잭션)
            int inserted = insertBatch(messages);
            log.info("Message insert: expected={}, actual={}", messages.size(), inserted);

            if (inserted == messages.size()) {
                return; // 정상 완료
            }

            // 4. 누락만 재시도
            Set<Long> savedBillingIds =
                    messageRepository.findExistingBillingIds(
                            target.stream().map(BillingSnapshot::getBillingId).toList()
                    );

            target = target.stream()
                    .filter(s -> !savedBillingIds.contains(s.getBillingId()))
                    .toList();

            log.warn("누락 {}건 재시도 ({}회차)", target.size(), retry);
        }

        if (!target.isEmpty()) {
            log.error("최종 실패 {}건", target.size());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int insertBatch(List<Message> messages) {
        return messageJdbcRepository.batchInsert(messages);
    }

    private LocalDateTime calculateScheduledTime(User user) {
        LocalDate today = LocalDate.now();
        int sendingDay = user.getSendingDay();

        LocalDate sendDate =
                today.getDayOfMonth() < sendingDay
                        ? today.withDayOfMonth(sendingDay)
                        : today.plusMonths(1).withDayOfMonth(sendingDay);

        LocalTime sendTime =
                user.getBanEndTime() != null
                        ? user.getBanEndTime().plusMinutes(1)
                        : LocalTime.of(9, 0);

        return LocalDateTime.of(sendDate, sendTime);
    }
}
