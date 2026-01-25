package com.touplus.billing_batch.domain.repository.serviceImpl;

import com.touplus.billing_batch.domain.entity.GroupDiscount;
import com.touplus.billing_batch.domain.repository.service.GroupDiscountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class GroupDiscountRepositoryImpl implements GroupDiscountRepository {
    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    /* ===============================
     * 공통 RowMapper
     * =============================== */
    private GroupDiscount mapRow(ResultSet rs, int rowNum) throws SQLException {
        return GroupDiscount.builder()
                .groupId(rs.getLong("group_id"))
                .numOfMember(rs.getInt("num_of_member"))
                .build();
    }

    @Override
    public List<GroupDiscount> findByUserIdIn(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }

        String sql = """
            SELECT
                gd.group_id,
                gd.num_of_member
            FROM group_discount gd
            WHERE gd.group_id IN (
                SELECT DISTINCT bu.group_id
                FROM billing_user bu
                WHERE bu.user_id IN (:userIds)
            )
        """;

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("userIds", userIds);

        return namedJdbcTemplate.query(sql, params, this::mapRow);
    }
}
