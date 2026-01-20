package com.touplus.billing_batch.message;

import com.touplus.billing_batch.BillingBatchApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.batch.core.*;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean; // 수정: @MockBean 대체
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;

@SpringBatchTest
@SpringBootTest(classes = BillingBatchApplication.class)
@ActiveProfiles("local")
public class MessageJobTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Job messageJob;

    // [수정] Spring Boot 3.4+ 버전의 새로운 표준 어노테이션 사용
    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @BeforeEach
    public void setup() {
        jobRepositoryTestUtils.removeJobExecutions();

        // [수정] CompletableFuture 관련 타입 모호성 및 메서드 매칭 해결
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(null);

        // doReturn 방식을 사용하면 타입 추론 에러를 더 안전하게 피할 수 있습니다.
        Mockito.doReturn(future).when(kafkaTemplate).send(anyString(), any());
        Mockito.doReturn(future).when(kafkaTemplate).send(anyString(), anyString(), any());

        // DB 초기화
        jdbcTemplate.execute("DELETE FROM batch_billing_error_log");
        jdbcTemplate.execute("DELETE FROM billing_result");

        for (int i = 1; i <= 5; i++) {
            jdbcTemplate.update(
                    "INSERT INTO billing_result (settlement_month, user_id, total_price, settlement_details, send_status, batch_execution_id) " +
                            "VALUES (?, ?, ?, ?, ?, ?)",
                    LocalDate.now().withDayOfMonth(1), (long) i, 10000 * i, "{}", "READY", 100L
            );
        }
    }

    @Test
    @DisplayName("메시지 발송 배치 통합 테스트: 발송 후 상태 업데이트 확인")
    void messageStepIntegrationTest() throws Exception {
        // given
        jobLauncherTestUtils.setJob(messageJob);
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        // when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // 비동기 작업 대기
        Thread.sleep(2000);

        // then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // 결과 확인
        List<Map<String, Object>> results = jdbcTemplate.queryForList("SELECT send_status FROM billing_result");

        System.out.println("### DB 상태 확인 ###");
        results.forEach(System.out::println);

        assertThat(results).isNotEmpty();
        assertThat(results).allSatisfy(row -> {
            String status = (String) row.get("send_status");
            assertThat(status).isEqualTo("SUCCESS");
        });
    }

    @Test
    @DisplayName("메시지 발송 실패 시 에러 로그 테이블 저장 테스트")
    void messageStepFailureTest() throws Exception {
        // 1. given: 어떤 send 메서드가 호출되더라도 에러를 던지도록 Mock 설정 강화
        Mockito.doThrow(new RuntimeException("Kafka 전송 강제 실패 테스트"))
                .when(kafkaTemplate).send(anyString(), any());
        Mockito.doThrow(new RuntimeException("Kafka 전송 강제 실패 테스트"))
                .when(kafkaTemplate).send(anyString(), any(), any()); // 인자가 3개인 경우 대비
        Mockito.doThrow(new RuntimeException("Kafka 전송 강제 실패 테스트"))
                .when(kafkaTemplate).send(anyString(), any(), any(), any()); // 인자가 4개인 경우 대비

        jobLauncherTestUtils.setJob(messageJob);
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis())
                .toJobParameters() ;

        // 2. when
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // 비동기 처리가 완료될 때까지 충분히 대기
        Thread.sleep(3000);

        // 3. then
        List<Map<String, Object>> errorLogs = jdbcTemplate.queryForList("SELECT * FROM batch_billing_error_log");

        System.out.println("### 에러 로그 테이블 확인 (건수: " + errorLogs.size() + ") ###");
        errorLogs.forEach(log -> System.out.println("에러 메시지: " + log.get("error_message")));

        // 로그 테이블에 데이터가 쌓였는지 검증
        assertThat(errorLogs).withFailMessage("에러 로그 테이블이 비어있습니다. Writer 로직을 확인하세요.")
                .isNotEmpty();
    }
}