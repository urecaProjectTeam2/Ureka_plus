package com.touplus.billing_batch.jobs.billing.step.writer;

import com.touplus.billing_batch.domain.entity.BillingResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import javax.sql.DataSource;


    // 기존에 JPA로 작성됨 --> JDBC로 변경
    // 영속성 비용 --> 메모리 부족남
    // settlement_details가 json 타입 --> JDBC 드라이버가 String을 json컬럼으로 넣을 수 있음

@Slf4j
@Configuration
@RequiredArgsConstructor
public class BillingItemWriterConfig {

    private final DataSource dataSource;

    @Value("#{jobParameters['chunkSize'] ?: 1000}")
    private int chunkSize;

    @Bean(name = "billingItemWriter")
    public JdbcBatchItemWriter<BillingResult> billingItemWriter() {
        /*
         * 변경 포인트(JPA --> JDBC)
         * 1. JPA 대신 JDBC 직접 사용: 영속성 컨텍스트 관리 비용 제거
         * 2. Bulk Insert: 쿼리 한 번에 1,000건씩 묶어서 전송 (rewriteBatchedStatements=true 설정과 결합 시 폭발적 성능)
         * 3. 테이블명: billing_result
         */
        log.info("Commit Billing Result Data: {}", chunkSize);
        return new JdbcBatchItemWriterBuilder<BillingResult>()
                .dataSource(dataSource)
                .sql("INSERT INTO billing_result " +
                        "(settlement_month, user_id, total_price, settlement_details, send_status, batch_execution_id, processed_at) " +
                        "VALUES (:settlementMonth, :userId, :totalPrice, :settlementDetails, :sendStatus, :batchExecutionId, :processedAt)")
                // .beanMapped() 대신 아래 코드를 사용하세요
                .itemSqlParameterSourceProvider(item -> {
                    MapSqlParameterSource params = new MapSqlParameterSource();
                    params.addValue("settlementMonth", item.getSettlementMonth());
                    params.addValue("userId", item.getUserId());
                    params.addValue("totalPrice", item.getTotalPrice());
                    params.addValue("settlementDetails", item.getSettlementDetails());
                    // Enum 객체를 .name()을 통해 순수 문자열(READY, SUCCESS 등)로 변환!
                    params.addValue("sendStatus", item.getSendStatus() != null ? item.getSendStatus().name() : "READY");
                    params.addValue("batchExecutionId", item.getBatchExecutionId());
                    params.addValue("processedAt", item.getProcessedAt());
                    return params;
                })
                .build();
    }
}