package com.touplus.billing_api.domain.message.service;

import com.touplus.billing_api.domain.message.dto.MessageStatusSummaryDto;

public interface MessageDashBoardService {
    MessageStatusSummaryDto getMessageStatusSummary();
}
