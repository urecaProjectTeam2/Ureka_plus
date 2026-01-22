package com.touplus.billing_api.domain.repository.billing.impl;

import com.touplus.billing_api.domain.billing.entity.BillingResult;
import com.touplus.billing_api.domain.billing.enums.SendStatus;
import com.touplus.billing_api.domain.repository.billing.BillingResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class BillingResultRepositoryImpl implements BillingResultRepository {

    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    private BillingResult mapRow(ResultSet rs, int rowNum) throws SQLException {
        return BillingResult.builder()
                .id(rs.getLong("billing_result_id"))
                .settlementMonth(
                        rs.getObject("settlement_month", LocalDate.class)
                )
                .userId(rs.getLong("user_id"))
                .totalPrice(rs.getInt("total_price"))
                .settlementDetails(rs.getString("settlement_details"))
                .sendStatus(
                        SendStatus.valueOf(rs.getString("send_status"))
                )
                .batchExecutionId(rs.getLong("batch_execution_id"))
                .processedAt(
                        rs.getObject("processed_at", LocalDateTime.class)
                )
                .build();
    }

    @Override
    public List<BillingResult> findBySendStatusForUpdate(SendStatus status) {
        return List.of();
    }

}
