package com.touplus.billing_message.service;

import com.touplus.billing_message.domain.respository.MessageProcessStatusRepository;
import com.touplus.billing_message.domain.respository.UserRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageProcessStatusService {

    private final MessageProcessStatusRepository messageProcessStatusRepository;
    private final UserRepository userRepository;

    public void initializeForRun(LocalDate settlementMonth) {
        if (settlementMonth == null) {
            log.warn("MessageProcess init skipped: settlementMonth is null");
            return;
        }

        long expectedTotal = userRepository.count();
        // 매 신호마다 누적을 막기 위해 카운터를 0부터 재설정
        messageProcessStatusRepository.resetForRun(settlementMonth, expectedTotal);
        log.info("MessageProcess reset: settlementMonth={}, expectedTotal={}",
                settlementMonth, expectedTotal);
    }

    public void increaseCreateCount(int delta) {
        if (delta <= 0) {
            return;
        }
        int updated = messageProcessStatusRepository.incrementCreate(delta);
        if (updated == 0) {
            log.warn("MessageProcess create_count not updated (row missing?) delta={}", delta);
        }
    }

    public void increaseSentCount(int delta) {
        if (delta <= 0) {
            return;
        }
        int updated = messageProcessStatusRepository.incrementSent(delta);
        if (updated == 0) {
            log.warn("MessageProcess sent_count not updated (row missing?) delta={}", delta);
        }
    }
}
