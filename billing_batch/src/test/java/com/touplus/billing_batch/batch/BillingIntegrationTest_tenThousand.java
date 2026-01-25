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
class BillingIntegrationTest_tenThousand {

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
        // 1. Given: 10,000건의 원천 데이터를 생성하는 로직이 필요하거나,
        // 이미 DB(원본 유저/청구 테이블)에 데이터가 있다는 가정하에 실행합니다.
        // 만약 테스트 코드에서 직접 데이터를 넣어야 한다면 여기서 10,000건 INSERT 로직을 수행하세요.

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("targetMonth", "2025-12-01")
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        // 2. When
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // 3. Then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // 결과 조회
        Integer successCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM billing_result WHERE settlement_month = '2025-12-01'", Integer.class);

        Integer distinctErrorUserCount = jdbcTemplate.queryForObject(
                "SELECT count(DISTINCT user_id) FROM batch_billing_error_log", Integer.class);

        System.out.println(">>> [BillingJob] 성공: " + successCount + ", 에러: " + distinctErrorUserCount);

        // 검증: 9,999명 성공 + 1명 에러 = 10,000명
        assertThat(successCount).isEqualTo(9999);
        assertThat(distinctErrorUserCount).isEqualTo(1);
        assertThat(successCount + distinctErrorUserCount).isEqualTo(10000);
    }
}