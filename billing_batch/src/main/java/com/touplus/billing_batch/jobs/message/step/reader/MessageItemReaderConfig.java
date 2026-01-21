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


        /*
         * [DDL 기반 수정 사항]
         * 1. 컬럼 매핑: DDL의 billing_result_id를 DTO의 id(또는 billingResultId)로 매핑
         * 2. JSON 컬럼: settlement_details는 문자열로 읽어온 뒤 DTO에서 파싱하도록 구성
         * 3. 인덱스 최적화: 유니크 키(uk_billing_month_user)가 있으므로 정렬 시 이 인덱스를 타도록 유도
         */

        // ----------------------------------------------------------
//                        billing_result 사용(실제)

//        return new JdbcCursorItemReaderBuilder<BillingResultDto>()
//                .name("messageReader")
//                .dataSource(dataSource)
//                .sql("SELECT billing_result_id AS id, " +
//                        "settlement_month, user_id, total_price, " +
//                        "send_status, batch_execution_id, processed_at " +
//                        "FROM billing_result " +
//                        "WHERE send_status = 'READY' " +
//                        "ORDER BY billing_result_id ASC")
//                .rowMapper(new BeanPropertyRowMapper<>(BillingResultDto.class))
//                .fetchSize(2000) // 개선: ChunkSize(1000)보다 크게 설정하여 네트워크 호출 감소
//                .saveState(false)
//                .build();
// ----------------------------------------------------------
//                      tmp_billing_result 사용(테스트)

        return new JdbcCursorItemReaderBuilder<BillingResultDto>()
                .name("messageReader")
                .dataSource(dataSource)
                .sql("SELECT " +
                        "billing_result_id AS id, " +           // PK 매핑
                        "settlement_month, " +                  // DATE 매핑
                        "user_id, " +                           // BIGINT 매핑
                        "total_price, " +                       // INT 매핑
                        "settlement_details, " +                // JSON 타입 (String으로 수신)
                        "send_status, " +                       // ENUM 매핑
                        "batch_execution_id, " +                // BIGINT 매핑
                        "processed_at " +                       // TIMESTAMP 매핑
                        "FROM tmp_billing_result " +
                        "WHERE send_status = 'READY' " +
                        "ORDER BY billing_result_id ASC")       // No-Offset 처럼 순차적 읽기 보장
                .rowMapper(new BeanPropertyRowMapper<>(BillingResultDto.class))
                .fetchSize(chunkSize)
                .saveState(false) // 대용량 처리 시 상태 저장을 꺼서 오버헤드 감소
                .build();
    }
}