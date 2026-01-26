package com.touplus.billing_message.consumer;

import com.touplus.billing_message.service.DispatchActivationFlag;
import com.touplus.billing_message.service.MessageProcessStatusService;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BatchSignalListener {

    private final KafkaListenerEndpointRegistry registry;
    private final DispatchActivationFlag dispatchActivationFlag;
    private final MessageProcessStatusService messageProcessStatusService;

    @KafkaListener(
            topics = "${message.signal.topic:billing-batch-done}",
            groupId = "billing-message-signal-group",
            containerFactory = "signalKafkaListenerContainerFactory")
    public void onSignal(String payload, Acknowledgment ack) {
        try {
            LocalDate settlementMonth = parseSettlementMonth(payload);
            if (settlementMonth != null) {
                messageProcessStatusService.initializeForRun(settlementMonth);
            } else {
                log.warn("[Signal] settlementMonth parse failed. payload={}", payload);
            }

            dispatchActivationFlag.enable();
            MessageListenerContainer container = registry.getListenerContainer("billingMessageListener");
            if (container != null && !container.isRunning()) {
                container.start();
                log.info("🔔[Signal] message listener started. payload={}", payload);
            } else {
                log.info("🔔[Signal] message listener already running. payload={}", payload);
            }
            ack.acknowledge();
        } catch (Exception e) {
            log.error("[Signal] processing failed", e);
        }
    }

    private LocalDate parseSettlementMonth(String payload) {
        if (payload == null || payload.isBlank()) {
            return null;
        }
        String[] parts = payload.split(":");
        if (parts.length < 2) {
            return null;
        }
        try {
            return LocalDate.parse(parts[1]);
        } catch (DateTimeParseException e) {
            // Fallback for yyMM / yyyyMM payloads, e.g. "2512" or "202512".
            try {
                String token = parts[1].trim();
                if (token.length() == 4) {
                    YearMonth ym = YearMonth.parse(token, DateTimeFormatter.ofPattern("yyMM"));
                    return ym.atDay(1);
                }
                if (token.length() == 6) {
                    YearMonth ym = YearMonth.parse(token, DateTimeFormatter.ofPattern("yyyyMM"));
                    return ym.atDay(1);
                }
            } catch (DateTimeParseException ignored) {
                // fall through
            }
            return null;
        }
    }
}
