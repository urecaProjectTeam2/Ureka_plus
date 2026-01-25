package com.touplus.billing_api.admin.repository;

import com.touplus.billing_api.domain.message.entity.User;
import java.time.LocalDate;
import java.util.List;

public interface UserSettlementRepository {
    List<User> findUsersBySettlementMonth(LocalDate settlementMonth, int page, int size);
    long countUsersBySettlementMonth(LocalDate settlementMonth);
}
