package com.touplus.billing_batch.domain.repository.serviceImpl;

import com.touplus.billing_batch.domain.dto.BillingUserMemberDto;
import com.touplus.billing_batch.domain.dto.MinMaxIdDto;
import com.touplus.billing_batch.domain.entity.BillingUser;
import com.touplus.billing_batch.domain.repository.service.BillingUserRepository;
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
public class BillingUserRepositoryImpl implements BillingUserRepository {

    private final NamedParameterJdbcTemplate namedJdbcTemplate;


    /* ===============================
     * 공통 RowMapper
     * =============================== */
    private BillingUserMemberDto mapRowGroup(ResultSet rs, int rowNum) throws SQLException {
        return BillingUserMemberDto.builder()
                .userId(rs.getLong("user_id"))
                .groupId(rs.getLong("group_id"))
                .userNumOfMember(rs.getInt("user_num_of_member"))       // 기존 COALESCE 값
                .groupNumOfMember(rs.getInt("group_num_of_member")) // 실제 DB 값
                .build();
    }

    private BillingUser mapUserIdOnly(ResultSet rs, int rowNum) throws SQLException {
        return BillingUser.builder()
                .userId(rs.getLong("user_id"))
                .build();
    }

    private BillingUser mapRow(ResultSet rs, int rowNum) throws SQLException {
        return BillingUser.builder()
                .userId(rs.getLong("user_id"))
                .groupId(rs.getLong("group_id"))
                .build();
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
    public List<BillingUserMemberDto> findUsersInRange(
            Long minValue,
            Long maxValue,
            Long lastProcessedUserId,
            boolean forceFullScan,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    ) {
        // JOIN을 통해 num_of_member를 가져오는 SQL
        String sql = """
            SELECT 
                b.user_id, 
                b.group_id, 
                COALESCE(g.num_of_member, 0) AS user_num_of_member,  -- 기존 COALESCE
                g.num_of_member AS group_num_of_member             -- 실제 테이블 값
            FROM billing_user b
            LEFT JOIN group_discount g ON b.group_id = g.group_id
            WHERE b.user_id BETWEEN :minValue AND :maxValue
              AND b.user_id > :lastProcessedUserId
              AND (
                  :forceFullScan = true
                  OR NOT EXISTS (
                      SELECT 1
                      FROM billing_result r
                      WHERE r.user_id = b.user_id
                        AND r.settlement_month BETWEEN :startDate AND :endDate
                  )
              )
            ORDER BY b.user_id ASC
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

        // 이전에 수정했던 mapUserIdOnly를 그대로 사용 (numOfMember 매핑 포함)
        return namedJdbcTemplate.query(sql, params, this::mapRowGroup);
    }

}
