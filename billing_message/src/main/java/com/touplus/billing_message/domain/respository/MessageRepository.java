package com.touplus.billing_message.domain.respository;

import com.touplus.billing_message.domain.entity.Message;
import com.touplus.billing_message.domain.entity.MessageStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Message Repository
 */
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * billingId로 Message 존재 여부 확인 (중복 체크용)
     */
    boolean existsByBillingId(Long billingId);

    /**
     * 배치 중복 체크용: 입력된 billingId 목록 중 이미 존재하는 ID 조회
     */
    @Query("SELECT m.billingId FROM Message m WHERE m.billingId IN :billingIds")
    Set<Long> findExistingBillingIds(@Param("billingIds") List<Long> billingIds);

    /**
     * WAITED 상태 메시지 ID 선점 (FOR UPDATE SKIP LOCKED)
     */
    @Query(value = """
                SELECT message_id
                FROM message
                WHERE status = 'WAITED'
                  AND (scheduled_at IS NULL OR scheduled_at <= :now)
                ORDER BY (scheduled_at IS NULL), scheduled_at, message_id
                LIMIT :limit
                FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<Long> lockNextMessageIds(@Param("now") LocalDateTime now, @Param("limit") int limit);

    /**
     * CREATED 상태로 변경 (snapshot 생성 완료)
     */
    @Modifying
    @Query(value = "UPDATE message SET status = 'CREATED' WHERE message_id = :id", nativeQuery = true)
    int markCreated(@Param("id") Long id);

    /**
     * CREATED 상태로 일괄 변경 (claim 시)
     */
    @Modifying
    @Query(value = """
                UPDATE message
                SET status = 'CREATED'
                WHERE message_id IN (:ids)
                  AND status = 'WAITED'
            """, nativeQuery = true)
    int markCreatedByIds(@Param("ids") List<Long> ids);

    /**
     * SENT 상태로 변경
     */
    @Modifying
    @Query(value = "UPDATE message SET status = 'SENT' WHERE message_id = :id", nativeQuery = true)
    int markSent(@Param("id") Long id);

    /**
     * 발송 연기 (금지 시간대)
     */
    @Modifying
    @Query(value = """
                UPDATE message
                SET status = 'WAITED', scheduled_at = :scheduledAt
                WHERE message_id = :id
            """, nativeQuery = true)
    int defer(@Param("id") Long id, @Param("scheduledAt") LocalDateTime scheduledAt);

    /**
     * 실패 처리 (재시도 예약)
     */
    @Modifying
    @Query(value = """
                UPDATE message
                SET status = 'WAITED', retry_count = retry_count + 1, scheduled_at = :scheduledAt
                WHERE message_id = :id
            """, nativeQuery = true)
    int markFailed(@Param("id") Long id, @Param("scheduledAt") LocalDateTime scheduledAt);

    /**
     * WAITED 상태 존재 여부 확인
     */
    boolean existsByStatus(MessageStatus status);

    /**
     * WAITED 상태 메시지 선점 (FOR UPDATE SKIP LOCKED)
     */
    @Query(value = """
                SELECT *
                FROM message
                WHERE status = 'WAITED'
                  AND (scheduled_at IS NULL OR scheduled_at <= :now)
                ORDER BY (scheduled_at IS NULL), scheduled_at, message_id
                LIMIT :limit
                FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<Message> lockNextMessages(@Param("now") LocalDateTime now, @Param("limit") int limit);
}
