package com.touplus.billing_batch.domain.repository;

import com.touplus.billing_batch.domain.entity.UserUsage;
import com.touplus.billing_batch.domain.enums.UseType;
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
public class UserUsageRepositoryImpl implements UserUsageRepository{
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
    public List<UserUsage> findByUserIdAndPeriod(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        String sql = """
            SELECT
                user_usage_id,
                user_id,
                use_month,
                use_type,
                use_amount
            FROM user_usage
            WHERE user_id = :userId
              AND use_month BETWEEN :startDate AND :endDate
            ORDER BY use_type
        """;


        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("startDate", startDate)
                .addValue("endDate", endDate);

        return namedJdbcTemplate.query(
                sql,
                params,
                this::mapRow
        );
    }
}
