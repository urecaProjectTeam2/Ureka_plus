package com.touplus.billing_batch.jobs.message.step.writer;

import com.touplus.billing_batch.domain.dto.BillingResultDto;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageItemWriter implements ItemWriter<BillingResultDto> {

    private final KafkaTemplate<String, Object> kafkaTemplate; // 본문
    private final JdbcTemplate jdbcTemplate;
    private final RetryTemplate retryTemplate;
    private static final String TOPIC = "billing-result-topic";

    @Override
    public void write(Chunk<? extends BillingResultDto> chunk) throws Exception {
        List<Long> successIds = new ArrayList<>();
        List<Long> failedIds = new ArrayList<>();

        for (BillingResultDto dto : chunk) {
            try {
                // kafka 전송 및 응답 확인 + 재시도 로직
                retryTemplate.execute(context -> {
                    // Kafka 전송 후 응답 대기 (.get())
                    kafkaTemplate.send(TOPIC, String.valueOf(dto.getUserId()), dto).get();
                    return null;
                });
                // 성공 리스트에 저장
                successIds.add(dto.getId());
            } catch (Exception e) {
                log.error("최종 발송 실패 - ID: {}", dto.getId());
                // 설정된 최대 재시도 횟수 도달 시 실패 목록에 추가
                failedIds.add(dto.getId());
            }
        }

        // 성공 데이터 DB 일괄 업데이트
        if (!successIds.isEmpty()) {
            updateSendStatus(successIds, "SUCCESS");
        }

        // 실패 데이터 DB 일괄 업데이트
        if (!failedIds.isEmpty()) {
            updateSendStatus(failedIds, "FAIL");
        }
    }

    private void updateSendStatus(List<Long> ids, String status) {
        // 실제 테이블
//        String sql = "UPDATE billing_result SET send_status = ?, processed_at = NOW() WHERE billing_result_id = ?";

        // 테스트 테이블
        String sql = "UPDATE tmp_billing_result SET send_status = ?, processed_at = NOW() WHERE billing_result_id = ?";
        jdbcTemplate.batchUpdate(sql, ids, ids.size(), (ps, id) -> {
            ps.setString(1, status);
            ps.setLong(2, id);
        });
    }
}