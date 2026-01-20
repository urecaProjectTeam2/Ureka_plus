package com.touplus.billing_message.service;

import com.touplus.billing_message.domain.entity.BillingSnapshot;
import com.touplus.billing_message.domain.entity.Message;
import com.touplus.billing_message.domain.entity.MessageSnapshot;
import com.touplus.billing_message.domain.entity.MessageTemplate;
import com.touplus.billing_message.domain.entity.MessageType;
import com.touplus.billing_message.domain.entity.User;
import com.touplus.billing_message.domain.respository.BillingSnapshotRepository;
import com.touplus.billing_message.domain.respository.MessageRepository;
import com.touplus.billing_message.domain.respository.MessageSnapshotRepository;
import com.touplus.billing_message.domain.respository.MessageTemplateRepository;
import com.touplus.billing_message.domain.respository.UserRepository;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
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

    /**
     * Message에 대한 MessageSnapshot 생성
     * 
     * @param message     발송 대상 메시지
     * @param messageType 발송 타입 (SMS/EMAIL)
     * @return 생성된 MessageSnapshot, 실패 시 empty
     */
    @Transactional
    public Optional<MessageSnapshot> createSnapshot(Message message, MessageType messageType) {
        Long messageId = message.getMessageId();
        Long billingId = message.getBillingId();
        Long userId = message.getUserId();

        // 이미 messageId로 snapshot이 존재하면 반환
        Optional<MessageSnapshot> existingById = messageSnapshotRepository.findById(messageId);
        if (existingById.isPresent()) {
            log.debug("Snapshot already exists for messageId={}", messageId);
            return existingById;
        }

        // BillingSnapshot 조회
        BillingSnapshot billing = billingSnapshotRepository.findById(billingId).orElse(null);
        if (billing == null) {
            log.warn("BillingSnapshot not found for billingId={}", billingId);
            return Optional.empty();
        }

        // user_id + settlement_month로 이미 snapshot이 존재하면 반환 (유니크 키 중복 방지)
        Optional<MessageSnapshot> existingByUserMonth = messageSnapshotRepository
                .findByUserIdAndSettlementMonth(userId, billing.getSettlementMonth());
        if (existingByUserMonth.isPresent()) {
            log.debug("Snapshot already exists for userId={} settlementMonth={}", userId, billing.getSettlementMonth());
            return existingByUserMonth;
        }

        // User 조회
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("User not found for userId={}", userId);
            return Optional.empty();
        }

        // MessageTemplate 조회
        MessageTemplate template = messageTemplateRepository.findByMessageType(messageType).orElse(null);
        if (template == null) {
            log.warn("MessageTemplate not found for messageType={}", messageType);
            return Optional.empty();
        }

        // 메시지 내용 생성
        String messageContent = buildMessageContent(template.getTemplateContent(), user, billing);

        // MessageSnapshot 생성
        MessageSnapshot snapshot = new MessageSnapshot(
                messageId,
                billingId,
                billing.getSettlementMonth(),
                userId,
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                billing.getTotalPrice(),
                billing.getSettlementDetails(),
                messageContent);

        messageSnapshotRepository.save(snapshot);

        // 메시지 상태를 CREATED로 변경
        messageRepository.markCreated(messageId);
        log.info("Created snapshot for messageId={} messageType={}, status changed to CREATED", messageId, messageType);

        return Optional.of(snapshot);
    }

    /**
     * 템플릿에 변수를 치환하여 메시지 내용 생성
     */
    private String buildMessageContent(String template, User user, BillingSnapshot billing) {
        String formattedMonth = billing.getSettlementMonth().format(MONTH_FORMATTER);
        String formattedPrice = PRICE_FORMATTER.format(billing.getTotalPrice());

        return template
                .replace("{userName}", user.getName())
                .replace("{userEmail}", user.getEmail())
                .replace("{userPhone}", user.getPhone())
                .replace("{settlementMonth}", formattedMonth)
                .replace("{totalPrice}", formattedPrice)
                .replace("{settlementDetails}", billing.getSettlementDetails());
    }
}
