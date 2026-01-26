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
        private final MessageSnapshotJdbcRepository messageSnapshotJdbcRepository;
        private final BillingSnapshotRepository billingSnapshotRepository;
        private final UserRepository userRepository;
        private final MessageTemplateRepository messageTemplateRepository;
        private final MessageRepository messageRepository;
        private final MessageProcessStatusService messageProcessStatusService;

        private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy년 MM월");
        private static final NumberFormat PRICE_FORMATTER = NumberFormat.getNumberInstance(Locale.KOREA);

        // 메시지 스냅샷을 배치로 생성
        @Transactional
        public int createSnapshotsBatch(List<Message> messages, MessageType messageType) {

                if (messages.isEmpty()) {
                        return 0;
                }

                List<Long> messageIds = new ArrayList<>(messages.size());
                Set<Long> billingIds = new HashSet<>();
                Set<Long> userIds = new HashSet<>();

                for (Message m : messages) {
                    messageIds.add(m.getMessageId());
                    billingIds.add(m.getBillingId());
                    userIds.add(m.getUserId());
                }

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

                MessageTemplate template = messageTemplateRepository.findFirstByMessageType(messageType)
                                .orElseThrow(() -> new IllegalStateException(
                                                "MessageTemplate not found: " + messageType));

                // 중복 생성된 스냅샷 제외
                Set<Long> existingMessageIds = messageSnapshotRepository.findExistingMessageIds(messageIds);

                // 스냅샷 생성
                List<MessageSnapshot> snapshots = new ArrayList<>(messages.size());

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
                messageProcessStatusService.increaseCreateCount(snapshots.size());

                // [Redis 기반] markCreatedByIds 제거 - WAITED → SENT로 직접 변경됨

                log.info("MessageSnapshot batch created: {}", snapshots.size());
                return snapshots.size();
        }

        /**
         * 스냅샷 배치 생성 (최적화 버전)
         * - 외부에서 userMap, billingMap 전달받아 중복 조회 제거
         * - JPA saveAll → JDBC batchInsert로 변경 (10배 이상 빠름)
         */
        @Transactional
        public int createSnapshotsBatchOptimized(
                List<Message> messages,
                MessageType messageType,
                Map<Long, User> userMap,
                Map<Long, BillingSnapshot> billingMap) {

                if (messages.isEmpty()) {
                        return 0;
                }

                List<Long> messageIds = messages.stream()
                        .map(Message::getMessageId)
                        .toList();

                // 템플릿 조회 (1회)
                MessageTemplate template = messageTemplateRepository.findFirstByMessageType(messageType)
                        .orElseThrow(() -> new IllegalStateException(
                                "MessageTemplate not found: " + messageType));

                // 중복 생성된 스냅샷 제외
                Set<Long> existingMessageIds = messageSnapshotRepository.findExistingMessageIds(messageIds);

                // 스냅샷 생성
                List<MessageSnapshot> snapshots = new ArrayList<>(messages.size());

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

                // JDBC batchInsert (JPA saveAll 대비 10배 이상 빠름)
                int inserted = messageSnapshotJdbcRepository.batchInsert(snapshots);
                messageProcessStatusService.increaseCreateCount(inserted);

                log.info("MessageSnapshot JDBC batch created: {}", inserted);
                return inserted;
        }

    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    // 템플릿 적용
    private String buildMessageContent(
            String template,
            User user,
            BillingSnapshot billing) {

        String result = template;
        result = result.replace("{userName}", user.getName());
        result = result.replace("{userEmail}", user.getEmail());
        result = result.replace("{userPhone}", user.getPhone());
        result = result.replace("{settlementMonth}",
                billing.getSettlementMonth().format(MONTH_FORMATTER));
        result = result.replace("{totalPrice}",
                PRICE_FORMATTER.format(billing.getTotalPrice()));
        
        // JSON 포맷팅 (원시 JSON -> 예쁜 문자열)
        String detailsFormatted = formatSettlementDetails(billing.getSettlementDetails());
        result = result.replace("{settlementDetails}", detailsFormatted);

        return result;
    }
    
    /**
     * JSON 상세 내역을 보기 좋은 문자열로 변환
     */
    private String formatSettlementDetails(String jsonDetails) {
        if (jsonDetails == null || jsonDetails.isBlank() || "{}".equals(jsonDetails)) {
            return "상세 내역 없음";
        }

        try {
            // JSON 파싱
            SettlementDetailsDto details = objectMapper.readValue(jsonDetails, SettlementDetailsDto.class);
            StringBuilder sb = new StringBuilder();

            // 1. Mobile (모바일)
            appendSection(sb, "📱 모바일", details.mobile());
            
            // 2. DPS (인터넷/TV)
            appendSection(sb, "🌐 인터넷/TV", details.dps());
            
            // 3. Addon (부가서비스)
            appendSection(sb, "➕ 부가서비스", details.addon());
            
            // 4. Discounts (할인)
            appendSection(sb, "📉 할인 내역", details.discounts());

            return sb.toString().trim();

        } catch (Exception e) {
            log.warn("JSON 상세 내역 파싱 실패: {}", jsonDetails, e);
            return jsonDetails; // 실패 시 원본 그대로 노출
        }
    }

    private void appendSection(StringBuilder sb, String title, List<DetailItem> items) {
        if (items != null && !items.isEmpty()) {
            sb.append("\n[").append(title).append("]\n");
            for (DetailItem item : items) {
                sb.append("- ").append(item.productName())
                  .append(" : ").append(PRICE_FORMATTER.format(item.price())).append("원\n");
            }
        }
    }

    // 내부 DTO 레코드
    private record SettlementDetailsDto(
        List<DetailItem> dps,
        List<DetailItem> addon,
        List<DetailItem> mobile,
        List<DetailItem> discounts
    ) {}

    private record DetailItem(
        int price,
        String productName,
        String productType
    ) {}

}
