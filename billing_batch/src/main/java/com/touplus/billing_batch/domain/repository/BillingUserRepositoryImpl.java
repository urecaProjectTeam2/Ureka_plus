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
        String sql = "SELECT MIN(user_id) as min_id, MAX(user_id) as max_id FROM billing_user";

        return namedJdbcTemplate.queryForObject(sql, new MapSqlParameterSource(), (rs, rowNum) ->
                MinMaxIdDto.builder()
                        .minId(getLong("min_id"))
                        .maxId(getLong("max_id"))
                        .build()
        );
    }
}
