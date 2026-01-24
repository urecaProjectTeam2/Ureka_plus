package com.touplus.billing_batch.domain.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserUsageRepositoryImpl implements UserUsageRepository{
    private final NamedParameterJdbcTemplate namedJdbcTemplate;
}
