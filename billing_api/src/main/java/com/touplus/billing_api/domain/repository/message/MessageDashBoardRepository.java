package com.touplus.billing_api.domain.repository.message;

import java.time.LocalDate;

public interface MessageDashBoardRepository {

    long countBySettlementMonth(LocalDate settlementMonth);

    long countBySettlementMonthAndStatus(
            LocalDate settlementMonth,
            String status
    );
    
    long countByStatusAndRetry();
    long countBySMS();
    
}
