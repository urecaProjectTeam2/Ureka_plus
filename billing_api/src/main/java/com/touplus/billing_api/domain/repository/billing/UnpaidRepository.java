package com.touplus.billing_api.domain.repository.billing;


import com.touplus.billing_api.domain.billing.entity.Unpaid;

import java.time.LocalDate;
import java.util.List;

public interface UnpaidRepository {

    List<Unpaid> findUnpaidUsers(int page, int size);
    List<Unpaid> findUnpaidUsersByMonth(int page, int size, String month);

    List<Unpaid> searchUnpaidUsersByKeyword(int page, int size, String keyword);
}
