package com.touplus.billing_api.domain.repository.message.impl;

import com.touplus.billing_api.domain.message.entity.User;
import com.touplus.billing_api.domain.message.enums.MessageType;
import com.touplus.billing_api.domain.repository.message.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final RowMapper<User> USER_ROW_MAPPER = (rs, rowNum) ->
            new User(
                    rs.getLong("user_id"),
                    rs.getString("name"),
                    rs.getString("email"),
                    rs.getString("phone"),
                    rs.getInt("sending_day"),
                    rs.getTime("ban_start_time") != null
                            ? rs.getTime("ban_start_time").toLocalTime()
                            : null,
                    rs.getTime("ban_end_time") != null
                            ? rs.getTime("ban_end_time").toLocalTime()
                            : null,
                    MessageType.valueOf(rs.getString("message_type"))
            );

    @Override
    public Optional<User> findById(Long userId) {
        String sql = """
            SELECT *
            FROM billing_message.users
            WHERE user_id = :userId
        """;

        List<User> result = jdbcTemplate.query(
                sql,
                Map.of("userId", userId),
                USER_ROW_MAPPER
        );

        return result.stream().findFirst();
    }

    @Override
    public List<User> findByIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }

        String sql = """
            SELECT *
            FROM billing_message.users
            WHERE user_id IN (:userIds)
        """;

        return jdbcTemplate.query(
                sql,
                Map.of("userIds", userIds),
                USER_ROW_MAPPER
        );
    }

    @Override
    public List<User> findAllPaged(int offset, int limit) {
        String sql = """
            SELECT *
            FROM billing_message.users
            ORDER BY user_id
            LIMIT :limit OFFSET :offset
        """;

        return jdbcTemplate.query(
                sql,
                Map.of(
                        "limit", limit,
                        "offset", offset
                ),
                USER_ROW_MAPPER
        );
    }

    @Override
    public long countAll() {
        String sql = """
            SELECT COUNT(*)
            FROM billing_message.users
        """;

        return jdbcTemplate.queryForObject(sql, Map.of(), Long.class);
    }
}