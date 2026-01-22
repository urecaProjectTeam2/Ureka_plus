package com.touplus.billing_message.service;

import com.touplus.billing_message.domain.entity.*;
import com.touplus.billing_message.domain.respository.*;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageSnapshotService {

        private final MessageSnapshotRepository messageSnapshotRepository;
        private final BillingSnapshotRepository billingSnapshotRepository;
        private final UserRepository userRepository;
        private final MessageTemplateRepository messageTemplateRepository;
        private final MessageRepository messageRepository;

        private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy년 MM월");
        private static final NumberFormat PRICE_FORMATTER = NumberFormat.getNumberInstance(Locale.KOREA);

        // 메시지 스냅샷을 배치로 생성
        @Transactional
        public int createSnapshotsBatch(List<Message> messages, MessageType messageType) {

                if (messages.isEmpty()) {
                        return 0;
                }

                // id들 수집 -> 근데 gpt 코드라 좀 더 쉽고 간편한 코드가 있지 않을까?하는 생각이 듦
                List<Long> messageIds = messages.stream()
                                .map(Message::getMessageId)
                                .toList();

                List<Long> billingIds = messages.stream()
                                .map(Message::getBillingId)
                                .distinct()
                                .toList();

                List<Long> userIds = messages.stream()
                                .map(Message::getUserId)
                                .distinct()
                                .toList();

                // 여기도 위랑 동일하게 생각 중
                Map<Long, BillingSnapshot> billingMap = billingSnapshotRepository.findAllById(billingIds)
                                .stream()
                                .collect(Collectors.toMap(
                                                BillingSnapshot::getBillingId,
                                                b -> b));

                Map<Long, User> userMap = userRepository.findAllById(userIds)
                                .stream()
                                .collect(Collectors.toMap(
                                                User::getUserId,
                                                u -> u));

                MessageTemplate template = messageTemplateRepository.findByMessageType(messageType)
                                .orElseThrow(() -> new IllegalStateException(
                                                "MessageTemplate not found: " + messageType));

                // 중복 생성된 스냅샷 제외
                Set<Long> existingMessageIds = messageSnapshotRepository.findExistingMessageIds(messageIds);

                // 스냅샷 생성
                List<MessageSnapshot> snapshots = new ArrayList<>();

                for (Message message : messages) {

                        Long messageId = message.getMessageId();
                        if (existingMessageIds.contains(messageId)) {
                                continue;
                        }

                        BillingSnapshot billing = billingMap.get(message.getBillingId());
                        User user = userMap.get(message.getUserId());

                        if (billing == null || user == null) {
                                log.warn("Snapshot skip - billing or user missing. messageId={}", messageId);
                                continue;
                        }

                        String content = buildMessageContent(
                                        template.getTemplateContent(),
                                        user,
                                        billing);

                        snapshots.add(new MessageSnapshot(
                                        messageId,
                                        billing.getBillingId(),
                                        billing.getSettlementMonth(),
                                        user.getUserId(),
                                        user.getName(),
                                        user.getEmail(),
                                        user.getPhone(),
                                        billing.getTotalPrice(),
                                        billing.getSettlementDetails(),
                                        content));
                }

                if (snapshots.isEmpty()) {
                        return 0;
                }

                // 스냅샷 삽입
                messageSnapshotRepository.saveAll(snapshots);

                // 스냅샷 업데이트
                List<Long> createdMessageIds = snapshots.stream()
                                .map(MessageSnapshot::getMessageId)
                                .toList();

                messageRepository.markCreatedByIds(createdMessageIds);

                log.info("MessageSnapshot batch created: {}", snapshots.size());
                return snapshots.size();
        }

        // 템플릿 적용
        private String buildMessageContent(
                        String template,
                        User user,
                        BillingSnapshot billing) {
                return template
                                .replace("{userName}", user.getName())
                                .replace("{userEmail}", user.getEmail())
                                .replace("{userPhone}", user.getPhone())
                                .replace("{settlementMonth}",
                                                billing.getSettlementMonth().format(MONTH_FORMATTER))
                                .replace("{totalPrice}",
                                                PRICE_FORMATTER.format(billing.getTotalPrice()))
                                .replace("{settlementDetails}",
                                                billing.getSettlementDetails());
        }

}
