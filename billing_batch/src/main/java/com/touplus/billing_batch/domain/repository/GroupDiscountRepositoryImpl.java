package com.touplus.billing_batch.domain.repository;

import com.touplus.billing_batch.domain.entity.GroupDiscount;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class GroupDiscountRepositoryImpl implements GroupDiscountRepository{
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
    public Optional<GroupDiscount> findByGroupId(Long groupId) {

        String sql ="""
            SELECT
                group_id,
                num_of_member
            FROM group_discount
            WHERE group_id = :groupId
        """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("groupId", groupId);

        return namedJdbcTemplate.query(sql, params, this::mapRow)
                .stream()
                .findFirst();
    }
}
