package com.touplus.billing_batch.jobs.message.step.reader;

import com.touplus.billing_batch.domain.dto.BillingResultDto;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.BeanPropertyRowMapper;

import javax.sql.DataSource;

@Configuration
public class MessageItemReaderConfig {

    private final int chunkSize = 2000;

    @Bean
    @StepScope
    public JdbcCursorItemReader<BillingResultDto> messageReader(DataSource dataSource) {
        /*
         *                        읽 기 전 략
         * No-Offset 전략:
         * 1. Paging 방식 사용 x
         *      --> 뒤로 갈 수록 이미 읽은 DB 쓰레기 읽는 시간 증가 // 성능 악화
         *      --> 데이터를 한 번에 메모리에 올리게 되면, 서버에 부하가 높아짐
         * 2. Cursor 방식 사용 o
         *       --> DB 커넥션 유지하면서 스트리밍 방식으로 읽어옴
         *       --> 데이터를 한 번에 메모리에 불러오지 않고, Fetch size만큼만 가져옴
         * 3. DB 락 방지 (멀티스레드 사용 시 별도 처리 필요)
         *      --> 내부적으로 ResultSet사용 --> synchronized 되어 있어, 스레드가 겹치는 부분을 건들이지 않음
         */
        return new JdbcCursorItemReaderBuilder<BillingResultDto>()
                .name("messageReader")
                .dataSource(dataSource)
                .sql("SELECT billing_result_id AS id, " +
                        "settlement_month, user_id, total_price, " +
                        "send_status, batch_execution_id, processed_at " +
                        "FROM billing_result " +
                        "WHERE send_status = 'READY' " +
                        "ORDER BY billing_result_id ASC")
                .rowMapper(new BeanPropertyRowMapper<>(BillingResultDto.class))
                .fetchSize(2000) // 개선: ChunkSize(1000)보다 크게 설정하여 네트워크 호출 감소
                .saveState(false)
                .build();
    }
}