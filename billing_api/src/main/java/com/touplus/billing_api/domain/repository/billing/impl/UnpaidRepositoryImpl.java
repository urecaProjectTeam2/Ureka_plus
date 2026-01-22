package com.touplus.billing_api.domain.repository.billing.impl;


import com.touplus.billing_api.domain.billing.entity.Unpaid;
import com.touplus.billing_api.domain.repository.billing.UnpaidRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
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

    private static final RowMapper<Unpaid> UNPAID_ROW_MAPPER =
            (rs, rowNum) -> Unpaid.builder()
                    .id(rs.getLong("unpaid_id"))
                    .userId(rs.getLong("user_id"))
                    .unpaidPrice(rs.getInt("unpaid_price"))
                    .unpaidMonth(rs.getObject("unpaid_month", LocalDate.class))
                    .paid(rs.getBoolean("is_paid"))
                    .build();

    @Override
    public List<Unpaid> findUnpaidUsers() {
        String sql = """
            SELECT unpaid_id,
                   user_id,
                   unpaid_price,
                   unpaid_month,
                   is_paid
            FROM unpaid
            WHERE is_paid = false
        """;

        return namedJdbcTemplate.query(sql, UNPAID_ROW_MAPPER);
    }
}
