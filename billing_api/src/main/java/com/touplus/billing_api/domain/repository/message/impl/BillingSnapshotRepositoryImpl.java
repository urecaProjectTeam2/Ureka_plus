package com.touplus.billing_api.domain.repository.message.impl;

import com.touplus.billing_api.domain.message.entity.BillingSnapshot;
import com.touplus.billing_api.domain.repository.message.BillingSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class BillingSnapshotRepositoryImpl implements BillingSnapshotRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final RowMapper<BillingSnapshot> BILLING_SNAPSHOT_ROW_MAPPER =
            new RowMapper<>() {
                @Override
                public BillingSnapshot mapRow(ResultSet rs, int rowNum) throws SQLException {
                    return new BillingSnapshot(
                            rs.getLong("billing_id"),
                            rs.getDate("settlement_month").toLocalDate(),
                            rs.getLong("user_id"),
                            rs.getInt("total_price"),
                            rs.getString("settlement_details")
                    );
                }
            };

    @Override
    public List<BillingSnapshot> findBySettlementMonth(LocalDate settlementMonth) {
        String sql = """
            SELECT *
            FROM billing_message.billing_snapshot
            WHERE settlement_month = :settlementMonth
        """;

        return jdbcTemplate.query(
                sql,
                Map.of("settlementMonth", settlementMonth),
                BILLING_SNAPSHOT_ROW_MAPPER
        );
    }

    @Override
    public List<Long> findUserIdsBySettlementMonth(LocalDate settlementMonth) {
        String sql = """
            SELECT user_id
            FROM billing_message.billing_snapshot
            WHERE settlement_month = :settlementMonth
        """;

        return jdbcTemplate.queryForList(
                sql,
                Map.of("settlementMonth", settlementMonth),
                Long.class
        );
    }
}
