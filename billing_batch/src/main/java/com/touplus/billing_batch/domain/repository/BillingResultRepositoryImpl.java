package com.touplus.billing_batch.domain.repository;

import com.touplus.billing_batch.domain.entity.BillingResult;
import com.touplus.billing_batch.domain.enums.SendStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class BillingResultRepositoryImpl implements BillingResultRepository {

    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    /* ===============================
     * 공통 RowMapper
     * =============================== */
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

    /**
     * JPA:
     * @Lock(PESSIMISTIC_WRITE)
     * List<BillingResult> findBySendStatusOrderById(SendStatus status)
     */
    @Override
    public List<BillingResult> findBySendStatusForUpdate(SendStatus status) {

        String sql = """
            SELECT
                billing_result_id,
                settlement_month,
                user_id,
                total_price,
                settlement_details,
                send_status,
                batch_execution_id,
                processed_at
            FROM tmp_billing_result
            WHERE send_status = :sendStatus
            ORDER BY billing_result_id
            FOR UPDATE
        """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("sendStatus", status.name());

        return namedJdbcTemplate.query(sql, params, this::mapRow);
    }

    @Override
    public void saveAll(List<BillingResult> results) throws Exception {

        String sql = """
            INSERT INTO tmp_billing_result
            (settlement_month, user_id, total_price, settlement_details, send_status, batch_execution_id, processed_at)
            VALUES
            (:settlementMonth, :userId, :totalPrice, :settlementDetails, :sendStatus, :batchExecutionId, :processedAt)
            ON DUPLICATE KEY UPDATE
                settlement_month = VALUES(settlement_month),
                total_price = VALUES(total_price),
                settlement_details = VALUES(settlement_details),
                send_status = VALUES(send_status),
                batch_execution_id = VALUES(batch_execution_id),
                processed_at = VALUES(processed_at)
        """;

        List<MapSqlParameterSource> batchParams = new ArrayList<>(results.size());

        for (BillingResult result : results) {
            batchParams.add(new MapSqlParameterSource()
                    .addValue("settlementMonth", result.getSettlementMonth())
                    .addValue("userId", result.getUserId())
                    .addValue("totalPrice", result.getTotalPrice())
                    .addValue("settlementDetails", result.getSettlementDetails())
                    .addValue("sendStatus", result.getSendStatus().name())
                    .addValue("batchExecutionId", result.getBatchExecutionId())
                    .addValue("processedAt", result.getProcessedAt())
            );
        }

        namedJdbcTemplate.batchUpdate(sql, batchParams.toArray(new MapSqlParameterSource[0]));
    }


}
