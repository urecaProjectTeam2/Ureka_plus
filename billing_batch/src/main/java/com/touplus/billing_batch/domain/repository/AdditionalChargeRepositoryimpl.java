package com.touplus.billing_batch.domain.repository;

import com.touplus.billing_batch.domain.entity.AdditionalCharge;
import com.touplus.billing_batch.domain.entity.BillingUser;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class AdditionalChargeRepositoryimpl implements AdditionalChargeRepository {

    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    // 공통 RowMapper
    private AdditionalCharge mapRow(ResultSet rs, int rowNum) throws SQLException {
        return AdditionalCharge.builder()
                .id(rs.getLong("additional_charge_id"))
                .companyName(rs.getString("company_name"))
                .price(rs.getInt("price"))
                .additionalChargeMonth(
                        rs.getDate("additional_charge_month").toLocalDate()
                )
                .userId(rs.getLong("user_id"))
                .build();
    }

    /**
     * JPA: findByUser
     */
    @Override
    public List<AdditionalCharge> findByUser(BillingUser user) {
        String sql = """
            SELECT *
            FROM additional_charge
            WHERE user_id = :userId
        """;

        MapSqlParameterSource params =
                new MapSqlParameterSource("userId", user.getUserId());

        return namedJdbcTemplate.query(sql, params, this::mapRow);
    }

    /**
     * JPA: findByAdditionalChargeMonth
     */
    @Override
    public List<AdditionalCharge> findByAdditionalChargeMonth(LocalDate month) {
        String sql = """
            SELECT *
            FROM additional_charge
            WHERE additional_charge_month = :month
        """;

        MapSqlParameterSource params =
                new MapSqlParameterSource("month", month);

        return namedJdbcTemplate.query(sql, params, this::mapRow);
    }

    /**
     * JPA: findByUserIdIn
     */
    @Override
    public List<AdditionalCharge> findByUserIdIn(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }

        String sql = """
            SELECT *
            FROM additional_charge
            WHERE user_id IN (:userIds)
        """;

        MapSqlParameterSource params =
                new MapSqlParameterSource("userIds", userIds);

        return namedJdbcTemplate.query(sql, params, this::mapRow);
    }
}
