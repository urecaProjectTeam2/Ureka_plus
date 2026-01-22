package com.touplus.billing_api.domain.repository.billing.impl;


import com.touplus.billing_api.domain.billing.entity.Unpaid;
import com.touplus.billing_api.domain.repository.billing.UnpaidRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class UnpaidRepositoryImpl implements UnpaidRepository {
    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    private Unpaid mapRow(ResultSet rs, int rowNum) throws SQLException {
        return Unpaid.builder()
                .id(rs.getLong("unpaid_id"))
                .unpaidPrice(rs.getInt("unpaid_price"))
                .unpaidMonth(
                        rs.getDate("unpaid_month").toLocalDate()
                )
                .paid(rs.getBoolean("is_paid"))
                .userId(rs.getLong("user_id"))
                .build();
    }

    @Override
    public List<Unpaid> findByPaidFalseAndUnpaidMonthBefore(LocalDate month) {
        return List.of();
    }

}
