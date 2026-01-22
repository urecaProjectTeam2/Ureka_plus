package com.touplus.billing_message.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.springframework.stereotype.Component;

@Component
public class MessagePolicy {

    /**
     * 현재 시간이 ban 시간대인지 확인
     * banEndTime만 사용 (scheduled_at이 이미 banEndTime+1분으로 설정되어 있으므로 보통 false)
     */
    public boolean isInBanWindow(LocalDateTime now, LocalTime banEndTime) {
        if (banEndTime == null) {
            return false;
        }
        LocalTime time = now.toLocalTime();
        // banEndTime 이전이면 ban 시간대
        return time.isBefore(banEndTime);
    }

    /**
     * 다음 발송 가능 시간 계산
     */
    public LocalDateTime nextAllowedTime(LocalDateTime now, LocalTime banEndTime) {
        if (banEndTime == null) {
            return now;
        }
        // banEndTime + 1분
        return now.toLocalDate().atTime(banEndTime).plusMinutes(1);
    }

    /**
     * 재시도 시간 계산
     */
    public LocalDateTime nextRetryAt(LocalDateTime now, int currentRetryCount) {
        int nextRetry = currentRetryCount + 1;
        if (nextRetry <= 1) {
            return now.plusMinutes(1);
        }
        if (nextRetry == 2) {
            return now.plusMinutes(3);
        }
        return now.plusMinutes(6);
    }

    /**
     * ban 시간대 고려하여 재시도 시간 조정
     */
    public LocalDateTime adjustForBan(LocalDateTime candidate, LocalTime banEndTime) {
        if (banEndTime == null) {
            return candidate;
        }
        if (!isInBanWindow(candidate, banEndTime)) {
            return candidate;
        }
        return nextAllowedTime(candidate, banEndTime);
    }
}

