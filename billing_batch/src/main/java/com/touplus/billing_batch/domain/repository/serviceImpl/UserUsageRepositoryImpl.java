package com.touplus.billing_batch.domain.repository.serviceImpl;

import com.touplus.billing_batch.domain.entity.UserUsage;
import com.touplus.billing_batch.domain.enums.UseType;
import com.touplus.billing_batch.domain.repository.service.UserUsageRepository;
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
public class UserUsageRepositoryImpl implements UserUsageRepository {
    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    private UserUsage mapRow(ResultSet rs, int rowNum) throws SQLException {
        return UserUsage.builder()
                .userUsageId(rs.getLong("user_usage_id"))
                .userId(rs.getLong("user_id"))
                .useMonth(rs.getDate("use_month").toLocalDate())
                .useType(UseType.valueOf(rs.getString("use_type")))
                .useAmount(rs.getInt("use_amount"))
                .build();
    }

    @Override
    public List<UserUsage> findByUserIdIn(
            List<Long> userIds,
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of(); // 빈 리스트 반환
        }

        String sql = """
        SELECT
            user_usage_id,
            user_id,
            use_month,
            use_type,
            use_amount
        FROM user_usage
        WHERE user_id IN (:userIds)
          AND use_month BETWEEN :startDate AND :endDate
        ORDER BY user_id, use_type
    """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userIds", userIds)
                .addValue("startDate", startDate)
                .addValue("endDate", endDate);

        return namedJdbcTemplate.query(sql, params, this::mapRow);
    }
}
