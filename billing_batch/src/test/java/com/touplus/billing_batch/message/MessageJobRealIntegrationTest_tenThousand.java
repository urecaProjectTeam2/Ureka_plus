package com.touplus.billing_batch.message;

import com.touplus.DotenvInitializer;
import com.touplus.billing_batch.BillingBatchApplication;
import jakarta.transaction.Transactional;
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
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBatchTest
@SpringBootTest(classes = BillingBatchApplication.class)
@ActiveProfiles("local")
@ContextConfiguration(initializers = DotenvInitializer.class)
public class MessageJobRealIntegrationTest_tenThousand {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Job messageJob;

//    @BeforeEach
//    public void setup() {
//        jobRepositoryTestUtils.removeJobExecutions();
//        jdbcTemplate.execute("DELETE FROM billing_result");
//
//        // 1. Given: 10,000건의 READY 데이터 생성 (대량 데이터 테스트 핵심)
//        System.out.println(">>> 테스트용 대량 데이터(10,000건)를 생성 중입니다...");
//        String sql = "INSERT INTO billing_result (settlement_month, user_id, total_price, settlement_details, send_status, batch_execution_id, processed_at) VALUES (?, ?, ?, ?, ?, ?, NOW())";
//
//        // Batch Update를 사용해 빠르게 10,000건 삽입
//        jdbcTemplate.batchUpdate(sql, new org.springframework.jdbc.core.BatchPreparedStatementSetter() {
//            @Override
//            public void setValues(java.sql.PreparedStatement ps, int i) throws java.sql.SQLException {
//                ps.setString(1, "2026-01-01");
//                ps.setLong(2, (long) (i + 1));
//                ps.setInt(3, 1000 * (i + 1));
//                ps.setString(4, "{\"test\":\"data\"}");
//                ps.setString(5, "READY");
//                ps.setLong(6, 1L);
//            }
//            @Override
//            public int getBatchSize() { return 10000; }
//        });
//        System.out.println(">>> 데이터 생성 완료.");
//    }

    @Test
    @DisplayName("10,000건 --> 대량 데이터 Kafka 전송 및 상태 업데이트 완결성 테스트")
    void bulkKafkaIntegrationTest() throws Exception {
        String targetMonth = LocalDate.now()
                .minusMonths(1)
                .format(DateTimeFormatter.ofPattern("yyMM")); // "2512"
        // 2. When
        jobLauncherTestUtils.setJob(messageJob);
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("settlementMonth", targetMonth)
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // 3. Wait: 10,000건 전송 및 업데이트 대기 (로컬 사양에 따라 조절)
        System.out.println(">>> Kafka 전송 및 DB 업데이트 대기 중 (15초)...");

        // 4. Then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        Integer finalSuccessCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM billing_result WHERE send_status = 'SUCCESS'", Integer.class);
        Integer finalReadyCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM billing_result WHERE send_status = 'READY'", Integer.class);

        System.out.println("==============================================");
        System.out.println("### 메시지 배치 최종 결과 요약 ###");
        System.out.println("- 전송 성공(SUCCESS): " + finalSuccessCount);
        System.out.println("- 남은 미처리(READY): " + finalReadyCount);
        System.out.println("==============================================");

        assertThat(finalSuccessCount).isEqualTo(9999);
        assertThat(finalReadyCount).isEqualTo(0);
    }
}