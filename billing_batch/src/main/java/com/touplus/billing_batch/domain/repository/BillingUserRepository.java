package com.touplus.billing_batch.domain.repository;

import com.touplus.billing_batch.domain.entity.BillingUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class BillingUserRepository {

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
}
