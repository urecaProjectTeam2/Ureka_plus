package com.touplus.billing_batch.jobs.message.step.writer;

import com.touplus.billing_batch.domain.dto.BillingResultDto;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/*
            *               쓰 기 전 략
            * 1. 비동기 방식 x
            *       --> 네트워크 지연으로 실패 시, 성공으로 저장됨을 방지
            * 2. 동기 방식 & Batch DB 일괄 업데이트  o
            *       --> 데이터 송신 후, .get()을 통해 답장이 올 때까지 기다림.
            *       --> 성공한 id에 대한 sync 를 받고,
            *       --> batch update를 통해, 한번에 1000개를 DB에 success라고 저장.
            * 3. DB 락 방지
            *       --> 인덱스 사용 --> 해당 row만 빠르게 수행
            *       --> 1000개의 chunk 단위 = 빠른 트랜잭션 가능
            * */

@Slf4j
@Component
@RequiredArgsConstructor
@StepScope
public class MessageItemWriter implements ItemWriter<BillingResultDto> {

    private final KafkaTemplate<String, Object> kafkaTemplate; // 본문
    private final JdbcTemplate jdbcTemplate;
    private final RetryTemplate retryTemplate;
    private static final String BASE_TOPIC = "billing-result-topic-";

    @Value("#{jobParameters['settlementMonth']}")
    private String settlementMonth;

    @Override
    public void write(Chunk<? extends BillingResultDto> chunk) throws Exception {
        String TOPIC = BASE_TOPIC + settlementMonth;
//        String TOPIC = BASE_TOPIC + settlementMonth +"T2";

        // 현재 청크 전송 결과 리스트
        List<CompletableFuture<?>> futures = new ArrayList<>();
        // 성공 ID를 담을 스레드 안전한 리스트
        List<Long> successIds = Collections.synchronizedList(new ArrayList<>());

        for (BillingResultDto dto : chunk) {
            // [시나리오 2] 1000개 chunk 비동기 발송 (Non-blocking)
            CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(TOPIC, String.valueOf(dto.getUserId()), dto);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    // [시나리오 2-1] ACK 수신 시 성공 리스트 추가
                    successIds.add(dto.getId());
                } else {
                    // [시나리오 2-2] NACK 발생 시 재시도 핸들러 호출
                    handleRetry(dto, TOPIC, successIds);
                }
            });
            futures.add(future);
        }

        // 현재 Chunk의 모든 Kafka 응답(또는 재시도 끝)이 올 때까지 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // [시나리오 2-1 & 2-2-1] 성공한 건들만 모아서 DB 일괄 업데이트
        if (!successIds.isEmpty()) {
            updateStatusBatch(successIds, "SUCCESS");
        }
    }

    private void handleRetry(BillingResultDto dto, String topic, List<Long> successIds) {
        try {
            // [시나리오 2-2-1] 재시도 3회 로직
            retryTemplate.execute(context -> {
                log.warn(">>> [Retry Attempt {}] ID: {}", context.getRetryCount() + 1, dto.getId());
                // 재시도 시에는 .get()을 사용하여 동기적으로 성공 여부 확인
                kafkaTemplate.send(topic, String.valueOf(dto.getUserId()), dto).get();
                successIds.add(dto.getId());
                return null;
            });
        } catch (Exception e) {
            // [시나리오 2-2-2] 3회 실패 시 즉시 FAIL 저장 (소수이므로 단건 처리)
            log.error(">>> [Final Fail] ID: {} - Error: {}", dto.getId(), e.getMessage());
            updateStatusSingle(dto.getId(), "FAIL");
        }
    }

    private void updateStatusBatch(List<Long> ids, String status) {
        String sql = "UPDATE billing_result SET send_status = ?, processed_at = NOW() WHERE billing_result_id = ?";
        jdbcTemplate.batchUpdate(sql, ids, ids.size(), (ps, id) -> {
            ps.setString(1, status);
            ps.setLong(2, id);
        });
    }

    private void updateStatusSingle(Long id, String status) {
        String sql = "UPDATE billing_result SET send_status = ?, processed_at = NOW() WHERE billing_result_id = ?";
        jdbcTemplate.update(sql, status, id);
    }
}