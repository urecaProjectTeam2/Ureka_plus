package com.touplus.billing_batch.jobs.message.step.reader;

import com.touplus.billing_batch.domain.dto.BillingResultDto;
import com.touplus.billing_batch.domain.enums.SendStatus;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.support.MySqlPagingQueryProvider;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.batch.item.support.builder.SynchronizedItemStreamReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class MessageItemReaderConfig {

    private final int chunkSize = 2500; // 1000 --> 2500개로 상향

    @Bean
    @StepScope
    public SynchronizedItemStreamReader<BillingResultDto> messageReader(DataSource dataSource) throws Exception {
        // 1. 실제 DB 페이징 읽기를 담당하는 리더 (내부에서만 사용됨)
        JdbcPagingItemReader<BillingResultDto> delegate = new JdbcPagingItemReaderBuilder<BillingResultDto>()
                .name("messageReaderDelegate")
                .dataSource(dataSource)
                .queryProvider(queryProvider())
                .pageSize(chunkSize)
                // BeanPropertyRowMapper 대신 아래와 같이 직접 매핑합니다.
                .rowMapper((rs, rowNum) -> BillingResultDto.builder()
                        .id(rs.getLong("billing_result_id"))
                        .settlementMonth(rs.getDate("settlement_month").toLocalDate())
                        .userId(rs.getLong("user_id"))
                        .totalPrice(rs.getInt("total_price"))
                        .settlementDetails(rs.getString("settlement_details"))
                        .sendStatus(SendStatus.valueOf(rs.getString("send_status")))
                        .batchExecutionId(rs.getLong("batch_execution_id"))
                        .processedAt(rs.getTimestamp("processed_at") != null ?
                                rs.getTimestamp("processed_at").toLocalDateTime() : null)
                        .build())
                .saveState(false) // 멀티스레드 환경이므로 상태 저장 안함
                .build();
        delegate.afterPropertiesSet();
        // 2. 여러 스레드가 동시에 읽어도 안전하도록 동기화 래퍼로 감싸서 반환
        return new SynchronizedItemStreamReaderBuilder<BillingResultDto>()
                .delegate(delegate)
                .build();
    }

    @Bean
    public MySqlPagingQueryProvider queryProvider() {
        MySqlPagingQueryProvider queryProvider = new MySqlPagingQueryProvider();

        // AS id 를 제거하고 순수하게 DB 컬럼명만 나열하세요. (문법 에러 방지)
        queryProvider.setSelectClause("billing_result_id, settlement_month, user_id, total_price, settlement_details, send_status, batch_execution_id, processed_at");
        queryProvider.setFromClause("billing_result");
        queryProvider.setWhereClause("send_status = 'READY'");

        Map<String, Order> sortKeys = new HashMap<>();
        sortKeys.put("billing_result_id", Order.ASCENDING);
        queryProvider.setSortKeys(sortKeys);

        return queryProvider;
    }
}