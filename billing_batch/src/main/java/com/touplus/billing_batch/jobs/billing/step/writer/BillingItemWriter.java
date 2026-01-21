package com.touplus.billing_batch.jobs.billing.step.writer;

import com.touplus.billing_batch.domain.entity.BillingResult;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;


    // 기존에 JPA로 작성됨 --> JDBC로 변경
    // 영속성 비용 --> 메모리 부족남
    // settlement_details가 json 타입 --> JDBC 드라이버가 String을 json컬럼으로 넣을 수 있음

@Configuration
@RequiredArgsConstructor
public class BillingItemWriter {

    private final DataSource dataSource;

    @Bean
    public JdbcBatchItemWriter<BillingResult> billingItemWriter() {
        /*
         * 변경 포인트(JPA --> JDBC)
         * 1. JPA 대신 JDBC 직접 사용: 영속성 컨텍스트 관리 비용 제거
         * 2. Bulk Insert: 쿼리 한 번에 1,000건씩 묶어서 전송 (rewriteBatchedStatements=true 설정과 결합 시 폭발적 성능)
         * 3. 테이블명: tmp_billing_result
         */
        return new JdbcBatchItemWriterBuilder<BillingResult>()
                .dataSource(dataSource)
                .sql("INSERT INTO tmp_billing_result " +
                        "(settlement_month, user_id, total_price, settlement_details, send_status, batch_execution_id, processed_at) " +
                        "VALUES (:settlementMonth, :userId, :totalPrice, :settlementDetails, :sendStatus.name, :batchExecutionId, :processedAt) " +
                        "ON DUPLICATE KEY UPDATE " + // 혹시 모를 재처리 시 덮어쓰기 전략
                        "total_price = VALUES(total_price), " +
                        "settlement_details = VALUES(settlement_details), " +
                        "send_status = VALUES(send_status), " +
                        "processed_at = VALUES(processed_at)")
                .beanMapped() // BillingResult 엔티티의 필드와 SQL 파라미터 자동 매핑
                .build();
    }
}