package com.touplus.billing_batch.domain.repository;

import com.touplus.billing_batch.domain.dto.MinMaxIdDto;
import com.touplus.billing_batch.domain.entity.BillingUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static java.lang.Long.getLong;

@Repository
@RequiredArgsConstructor
public class BillingUserRepositoryImpl implements BillingUserRepository{

    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    /* ===============================
     * 공통 RowMapper
     * =============================== */
    private BillingUser mapRow(ResultSet rs, int rowNum) throws SQLException {
        return BillingUser.builder()
                .userId(rs.getLong("user_id"))
                .build();
    }

    /**
     * JPA:
     * findUsersGreaterThanId(Long lastUserId, Pageable pageable)
     */
    @Override
    public List<BillingUser> findUsersGreaterThanId(
            Long lastUserId,
            Pageable pageable
    ) {

        String sql = """
            SELECT user_id
            FROM billing_user
            WHERE user_id > :lastUserId
            ORDER BY user_id ASC
            LIMIT :limit OFFSET :offset
        """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("lastUserId", lastUserId)
                .addValue("limit", pageable.getPageSize())
                .addValue("offset", pageable.getOffset());

        return namedJdbcTemplate.query(sql, params, this::mapRow);
    }

    @Override
    public MinMaxIdDto findMinMaxId() {
        String sql = "SELECT MIN(user_id) AS min_id, MAX(user_id) AS max_id FROM billing_user";

        return namedJdbcTemplate.queryForObject(
                sql,
                new MapSqlParameterSource(),
                (rs, rowNum) -> {
                    Long minId = rs.getLong("min_id");
                    if (rs.wasNull()) minId = null;

                    Long maxId = rs.getLong("max_id");
                    if (rs.wasNull()) maxId = null;

                    return MinMaxIdDto.builder()
                            .minId(minId)
                            .maxId(maxId)
                            .build();
                }
        );
    }

    @Override
    public List<BillingUser> findUsersInRange(
            Long minValue,
            Long maxValue,
            Long lastProcessedUserId,
            boolean forceFullScan,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    ) {

        String sql = """
            SELECT b.user_id
            FROM billing_user b
            WHERE b.user_id BETWEEN :minValue AND :maxValue
              AND b.user_id > :lastProcessedUserId
              /* forceFullScan이 true면 뒤의 NOT EXISTS 조건을 무시하고 전체 조회 */
              AND (
                  :forceFullScan = true
                  OR NOT EXISTS (
                      SELECT 1
                      FROM tmp_billing_result r
                      WHERE r.user_id = b.user_id
                        AND r.settlement_month BETWEEN :startDate AND :endDate
                  )
              )
            ORDER BY b.user_id
            LIMIT :limit
    """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("minValue", minValue)
                .addValue("maxValue", maxValue)
                .addValue("lastProcessedUserId", lastProcessedUserId)
                .addValue("forceFullScan", forceFullScan)
                .addValue("startDate", startDate)
                .addValue("endDate", endDate)
                .addValue("limit", pageable.getPageSize());

        return namedJdbcTemplate.query(sql, params, this::mapRow);
    }
}
