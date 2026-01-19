package com.touplus.billing_message.processor;

import com.touplus.billing_message.domain.entity.BillingSnapshot;
import com.touplus.billing_message.domain.entity.Message;
import com.touplus.billing_message.domain.entity.User;
import com.touplus.billing_message.domain.respository.MessageRepository;
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

    /**
     * BillingSnapshot을 받아 Message를 생성하고 저장
     */
    @Transactional
    public void process(BillingSnapshot snapshot) {
        // 1. 유저 정보 조회
        User user = userRepository.findById(snapshot.getUserId())
            .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다: " + snapshot.getUserId()));

        log.info("유저 정보 조회 완료: userId={}, sendingDay={}", user.getUserId(), user.getSendingDay());

        // 2. 발송 예정 시간 계산
        LocalDateTime scheduledAt = calculateScheduledTime(user);

        // 3. Message 저장
        Message message = new Message(
            snapshot.getBillingId(),
            snapshot.getUserId(),
            scheduledAt
        );

        messageRepository.save(message);
        log.info("Message 저장 완료: billingId={}, userId={}, scheduledAt={}",
            snapshot.getBillingId(), snapshot.getUserId(), scheduledAt);
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
