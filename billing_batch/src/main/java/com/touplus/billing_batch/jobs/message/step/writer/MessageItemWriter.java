package com.touplus.billing_batch.jobs.message.step.writer;

import com.touplus.billing_batch.domain.dto.BillingResultDto;
import lombok.RequiredArgsConstructor;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
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

@Component
@RequiredArgsConstructor
public class MessageItemWriter implements ItemWriter<BillingResultDto> {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final RetryTemplate retryTemplate;
    private static final String TOPIC = "billing-result-topic";

    @Override
    public void write(Chunk<? extends BillingResultDto> chunk) throws Exception {
        List<Long> successIds = Collections.synchronizedList(new ArrayList<>());
        List<BillingResultDto> failedItems = Collections.synchronizedList(new ArrayList<>());
        List<CompletableFuture<?>> futures = new ArrayList<>();

        for (BillingResultDto dto : chunk) {
            // 1. 비동기 + 재시도 로직 통합
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                retryTemplate.execute(context -> {
                    try {
                        // Kafka 전송 후 응답 대기 (.get())
                        kafkaTemplate.send(TOPIC, String.valueOf(dto.getUserId()), dto).get();
                        successIds.add(dto.getId());
                        return null;
                    } catch (Exception e) {
                        // 설정된 최대 재시도 횟수 도달 시 실패 목록에 추가
                        if (context.getRetryCount() >= 2) {
                            failedItems.add(dto);
                        }
                        throw new RuntimeException(e); // 재시도 트리거
                    }
                });
            });
            futures.add(future);
        }

        //  2. 모든 비동기 작업이 끝날 때까지 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        //  3. 성공 데이터 DB 일괄 업데이트
        if (!successIds.isEmpty()) {
            updateSendStatus(successIds, "SUCCESS");
        }

        //  4. 실패 데이터가 하나라도 있으면 예외를 던져 SkipListener 호출
        if (!failedItems.isEmpty()) {
            throw new RuntimeException("Kafka 최종 전송 실패 건 존재: " + failedItems.size() + "건");
        }
    }

    private void updateSendStatus(List<Long> ids, String status) {
        String sql = "UPDATE billing_result SET send_status = ?, processed_at = NOW() WHERE billing_result_id = ?";
        jdbcTemplate.batchUpdate(sql, ids, ids.size(), (ps, id) -> {
            ps.setString(1, status);
            ps.setLong(2, id);
        });
    }
}