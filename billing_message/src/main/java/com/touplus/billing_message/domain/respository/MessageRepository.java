package com.touplus.billing_message.domain.respository;

import com.touplus.billing_message.domain.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Message Repository
 * 메시지 저장용
 */
public interface MessageRepository extends JpaRepository<Message, Long> {
    
    /**
     * billingId로 Message 존재 여부 확인 (중복 체크용)
     */
    boolean existsByBillingId(Long billingId);

    /**
     * 배치 중복 체크용: 입력된 billingId 목록 중 이미 존재하는 ID 조회
     */
    @org.springframework.data.jpa.repository.Query("SELECT m.billingId FROM Message m WHERE m.billingId IN :billingIds")
    java.util.Set<Long> findExistingBillingIds(@org.springframework.data.repository.query.Param("billingIds") java.util.List<Long> billingIds);
}
