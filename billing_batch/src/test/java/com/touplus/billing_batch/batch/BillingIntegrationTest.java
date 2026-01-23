package com.touplus.billing_batch.batch;

import com.touplus.DotenvInitializer;
import com.touplus.billing_batch.TestBatchConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.*;
import org.springframework.batch.test.JobLauncherTestUtils; // 올바른 경로
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBatchTest
@SpringBootTest
@ActiveProfiles("local")
@Import(TestBatchConfig.class) // 위 설정 파일 가져옴
@ContextConfiguration(initializers = DotenvInitializer.class)
class BillingIntegrationTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    @Qualifier("billingJob") // 2. 우리가 만든 빈 이름을 정확히 지정해서 주입
    private Job billingJob;

    @BeforeEach
    public void setup() {
        // 3. 테스트 유틸에 실행할 Job을 수동으로 세팅해줍니다.
        jobLauncherTestUtils.setJob(billingJob);

        jdbcTemplate.execute("DELETE FROM batch_billing_error_log");
        jdbcTemplate.execute("DELETE FROM billing_result");
    }

    @Test
    void billingJobTest() throws Exception {
        // 1. Job 실행
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("settlementMonth", "2026-01-01")
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // 2. Job 상태 확인
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // [수정] 실제 데이터가 쌓이는 billing_result 테이블을 조회합니다.
        Integer successCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM billing_result WHERE settlement_month = '2026-01-01'",
                Integer.class
        );

        // 로깅을 추가해서 실제로 몇 건이 들어왔는지 눈으로 확인하면 더 좋습니다.
        System.out.println(">>> 성공 데이터 건수(billing_result): " + successCount);

        assertThat(successCount).isGreaterThan(0);

        // 3. Skip 로그 검증
        Integer errorLogCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM batch_billing_error_log WHERE user_id = ?",
                Integer.class,
                1659
        );

        // SkipListener에서 여러 번 찍힐 수 있으므로 최소 1건 이상인지 확인
        assertThat(errorLogCount)
                .as("Skip된 유저(1659)의 정보가 기록되어야 합니다.")
                .isGreaterThanOrEqualTo(1);

        // 4. 전체 정합성 확인 (성공 건수 + 에러 로그 기록 건수 = 전체 10,000건)
        // ※ 주의: errorLogCount가 중복 기록될 수 있다면 SELECT COUNT(DISTINCT user_id)를 고려하세요.
        Integer distinctErrorUserCount = jdbcTemplate.queryForObject(
                "SELECT count(DISTINCT user_id) FROM batch_billing_error_log", Integer.class);

        assertThat(successCount + distinctErrorUserCount).isEqualTo(10000);
    }
}