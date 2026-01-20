package com.touplus.billing_message.domain.respository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class UserBanRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserBanRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<UserBanInfo> findBanInfo(Long userId) {
        return jdbcTemplate.query(
                "SELECT ban_start_time, ban_end_time FROM users WHERE user_id = ?",
                new UserBanRowMapper(),
                userId
        ).stream().findFirst();
    }

    private static class UserBanRowMapper implements RowMapper<UserBanInfo> {
        @Override
        public UserBanInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
            LocalTime start = rs.getObject("ban_start_time", LocalTime.class);
            LocalTime end = rs.getObject("ban_end_time", LocalTime.class);
            return new UserBanInfo(start, end);
        }
    }
}
