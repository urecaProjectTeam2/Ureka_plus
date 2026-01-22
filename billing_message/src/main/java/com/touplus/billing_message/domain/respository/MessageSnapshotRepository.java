package com.touplus.billing_message.domain.respository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.touplus.billing_message.domain.entity.MessageSnapshot;

public interface MessageSnapshotRepository extends JpaRepository<MessageSnapshot, Long> {

    Optional<MessageSnapshot> findByUserIdAndSettlementMonth(Long userId, LocalDate settlementMonth);
    
    @Query("""
    	    SELECT ms.messageId
    	    FROM MessageSnapshot ms
    	    WHERE ms.messageId IN :messageIds
    	""")
    	Set<Long> findExistingMessageIds(@Param("messageIds") List<Long> messageIds);

}
