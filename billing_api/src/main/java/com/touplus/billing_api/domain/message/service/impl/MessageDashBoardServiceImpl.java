package com.touplus.billing_api.domain.message.service.impl;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

import com.touplus.billing_api.domain.message.dto.MessageStatusSummaryDto;
import com.touplus.billing_api.domain.message.service.MessageDashBoardService;
import com.touplus.billing_api.domain.repository.message.MessageDashBoardRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MessageDashBoardServiceImpl implements MessageDashBoardService {

    private final MessageDashBoardRepository messageDashBoardRepository;

    @Override
    public MessageStatusSummaryDto getMessageStatusSummary() {

        LocalDate lastMonthDate = getLastMonthDate();
        long retry = messageDashBoardRepository.countByStatusAndRetry();
        long sms = messageDashBoardRepository.countBySMS();

        long total = messageDashBoardRepository.countBySettlementMonth(lastMonthDate);
        
        long wait = messageDashBoardRepository.countBySettlementMonthAndStatus(lastMonthDate, "WAITED");
        long sent = messageDashBoardRepository.countBySettlementMonthAndStatus(lastMonthDate, "SENT");
        long fail = messageDashBoardRepository.countBySettlementMonthAndStatus(lastMonthDate, "FAILED");

        double waitRate = total == 0 ? 0 : (wait * 100.0 / total);
        double retryRate = total == 0 ? 0 : (retry * 100.0 / total);
        double sentRate = total == 0 ? 0 : (sent * 100.0 / total);
        double smsRate = total == 0 ? 0 : (sms * 100.0 / total);
        double failRate = total == 0 ? 0 : (fail * 100.0 / total);

        // yyyy-MM 포맷으로 변환
        String settlementMonthStr = lastMonthDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));

        return MessageStatusSummaryDto.builder()
                .totalCount(total)
                .waitCount(wait)
                .retryCount(retry)
                .sentCount(sent)
                .smsCount(sms)   
                .failCount(fail)        
                .waitRate(waitRate)     
                .retryRate(retryRate)     
                .sentRate(sentRate)
                .smsRate(smsRate)
                .failRate(failRate)
                .settlementMonth(settlementMonthStr)
                .build();
    }

    private LocalDate getLastMonthDate() {
        return LocalDate.now()
                .minusMonths(1)
                .withDayOfMonth(1);
    }
}