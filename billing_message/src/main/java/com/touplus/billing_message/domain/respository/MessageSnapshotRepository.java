package com.touplus.billing_message.domain.respository;

import com.touplus.billing_message.domain.entity.MessageSnapshot;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageSnapshotRepository extends JpaRepository<MessageSnapshot, Long> {

    Optional<MessageSnapshot> findByUserIdAndSettlementMonth(Long userId, LocalDate settlementMonth);
}
