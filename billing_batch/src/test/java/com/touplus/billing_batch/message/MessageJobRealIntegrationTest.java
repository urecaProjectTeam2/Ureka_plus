package com.touplus.billing_batch.message;

import com.touplus.billing_batch.BillingBatchApplication;
import com.touplus.DotenvInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.*;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBatchTest
@SpringBootTest(classes = BillingBatchApplication.class)
@ActiveProfiles("local")
@ContextConfiguration(initializers = DotenvInitializer.class)
public class MessageJobRealIntegrationTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Job messageJob;

    @BeforeEach
    public void setup() {
        jobRepositoryTestUtils.removeJobExecutions();


        jdbcTemplate.execute("DELETE FROM billing_result");

        // READY 상태의 테스트 데이터 5건 삽입
        // Job 파라미터와 일치시키기 위해 settlement_month를 고정하거나 현재 월로 설정
        String currentMonth = "2026-01-01";

        for (int i = 1; i <= 5; i++) {
            jdbcTemplate.update(
                    "INSERT INTO billing_result (settlement_month, user_id, total_price, settlement_details, send_status, batch_execution_id, processed_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?, NOW())",
                    currentMonth, (long) i, 10000 * i, "{\"test\":\"data\"}", "READY", 1L
            );
        }
    }

//    @Test
//    @DisplayName("5건 --> Kafka 전송 Job 실행 및 billing_result 상태 업데이트 검증")
//    void realKafkaIntegrationTest() throws Exception {
//        // 1. Given
//        jobLauncherTestUtils.setJob(messageJob);
//
//        // 만약 messageJob이 settlementMonth 파라미터를 받는다면 추가해줘야 합니다.
//        JobParameters jobParameters = new JobParametersBuilder()
//                .addString("settlementMonth", "2026-01-01")
//                .addLong("time", System.currentTimeMillis())
//                .toJobParameters();
//
//        // 2. When (실제 Kafka 발송 시작)
//        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);
//
//        // [중요] Kafka는 비동기로 동작하므로, DB 업데이트 시간을 벌어줘야 합니다.
//        // 데이터가 적으면 2~3초로 충분하지만 안전하게 유지합니다.
//        Thread.sleep(3000);
//
//        // 3. Then
//        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
//

//
//        System.out.println("### Kafka 전송 후 DB 업데이트 결과 ###");
//        results.forEach(row -> System.out.println("User: " + row.get("user_id") + ", Status: " + row.get("send_status")));
//
//        assertThat(results).hasSize(5);
//        assertThat(results).allSatisfy(row -> {
//            String status = (String) row.get("send_status");
//            // Kafka 전송 성공 후 SUCCESS로 업데이트 되었는지 검증
//            assertThat(status).isEqualTo("SUCCESS");
//        });
//    }

    // -----------------------------------------------------------------------------------------------------------------------------------

    @Test
    @DisplayName("10,000건 -->  대량 데이터 Kafka 전송 및 상태 업데이트 완결성 테스트")
    void bulkKafkaIntegrationTest() throws Exception {
        // 1. Given: 테스트 시작 전 현재 DB에 쌓여있는 READY 상태 데이터 건수 확인
        Integer initialReadyCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM billing_result WHERE send_status = 'READY'", Integer.class);

        System.out.println(">>> [시작] 전송 대기(READY) 데이터 건수: " + initialReadyCount);

        jobLauncherTestUtils.setJob(messageJob);
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("settlementMonth", "2026-01-01")
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        // 2. When: 실제 Kafka 발송 Job 실행
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // [중요] 만 건 단위는 Kafka 브로커로 전송하고 콜백을 받아 DB를 업데이트하는 데 시간이 걸립니다.
        // 로컬 성능에 따라 10~15초 정도 충분히 대기해 줍니다.
        System.out.println(">>> Kafka 전송 및 DB 업데이트 대기 중 (15초)...");
        Thread.sleep(15000);

        // 3. Then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // 최종 상태 집계 조회
        Integer totalCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM billing_result", Integer.class);

        Integer finalSuccessCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM billing_result WHERE send_status = 'SUCCESS'", Integer.class);

        Integer finalReadyCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM billing_result WHERE send_status = 'READY'", Integer.class);

        System.out.println("==============================================");
        System.out.println("### 최종 배치 처리 결과 요약 ###");
        System.out.println("- 전체 데이터 건수: " + totalCount);
        System.out.println("- 전송 성공(SUCCESS): " + finalSuccessCount);
        System.out.println("- 미처리(READY): " + finalReadyCount);
        System.out.println("==============================================");

        // 검증 1: DB에 데이터가 한 건이라도 있어야 함
        assertThat(totalCount).isGreaterThan(0);

        // 검증 2: 모든 READY 데이터가 SUCCESS로 바뀌었어야 함 (누락 확인)
        assertThat(finalSuccessCount)
                .as("모든 READY 데이터가 SUCCESS로 업데이트되어야 합니다.")
                .isEqualTo(totalCount);

        // 검증 3: READY 상태인 데이터는 0건이어야 함
        assertThat(finalReadyCount).isEqualTo(0);
    }
}