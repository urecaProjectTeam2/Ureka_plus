package com.touplus.billing_api.domain.repository.message;

import java.util.List;

import com.touplus.billing_api.domain.message.entity.Message;
import com.touplus.billing_api.domain.message.enums.MessageStatus;

public interface MessageRepository {

    // 1. 상태 기준 조회 (예: WAITED, FAILED)
    List<Message> findByStatus(MessageStatus status, int limit);

    // 2. message_id 목록으로 조회
    List<Message> findByMessageIds(List<Long> messageIds);

    // 3. user_id 기준 조회
    List<Message> findByUserId(Long userId);

    // 4. 발송 대상 조회 (스케줄 + 상태)
    List<Message> findSendableMessages(int limit);

    // 5. 상태 업데이트
    void updateStatus(Long messageId, MessageStatus status);

    // 6. retry count 증가
    void increaseRetryCount(Long messageId);
        
}
