package com.touplus.billing_message.service;

import com.touplus.billing_message.domain.respository.UserBanInfo;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.springframework.stereotype.Component;

@Component
public class MessagePolicy {

    public boolean isInBanWindow(LocalDateTime now, UserBanInfo banInfo) {
        if (banInfo == null || banInfo.start() == null || banInfo.end() == null) {
            return false;
        }
        LocalTime start = banInfo.start();
        LocalTime end = banInfo.end();
        LocalTime time = now.toLocalTime();

        if (start.equals(end)) {
            return true;
        }

        if (start.isBefore(end)) {
            return !time.isBefore(start) && time.isBefore(end);
        }
        return !time.isBefore(start) || time.isBefore(end);
    }

    public LocalDateTime nextAllowedTime(LocalDateTime now, UserBanInfo banInfo) {
        LocalTime start = banInfo.start();
        LocalTime end = banInfo.end();
        LocalTime time = now.toLocalTime();

        if (start.isBefore(end)) {
            return now.toLocalDate().atTime(end);
        }

        if (!time.isBefore(start)) {
            LocalDate nextDay = now.toLocalDate().plusDays(1);
            return nextDay.atTime(end);
        }
        return now.toLocalDate().atTime(end);
    }

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

    public LocalDateTime adjustForBan(LocalDateTime candidate, UserBanInfo banInfo) {
        if (banInfo == null) {
            return candidate;
        }
        if (!isInBanWindow(candidate, banInfo)) {
            return candidate;
        }
        return nextAllowedTime(candidate, banInfo);
    }
}
