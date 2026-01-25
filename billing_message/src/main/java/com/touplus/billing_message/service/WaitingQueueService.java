package com.touplus.billing_message.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.springframework.data.redis.core.ZSetOperations;

/**
 * Redis ZSet 기반 대기열 서비스
 * - score = 발송 예정 시간 (epoch seconds)
 * - value = messageId
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WaitingQueueService {

    private final StringRedisTemplate redisTemplate;
    private static final String QUEUE_KEY = "queue:message:waiting";

    /**
     * 대기열에 메시지 추가
     * @param messageId 메시지 ID
     * @param scheduledAt 발송 예정 시간 (null이면 즉시 발송)
     */
    public void addToQueue(Long messageId, LocalDateTime scheduledAt) {
        LocalDateTime releaseTime = scheduledAt != null ? scheduledAt : LocalDateTime.now();
        long score = releaseTime.atZone(ZoneId.systemDefault()).toEpochSecond();

        String value = String.valueOf(messageId);
        redisTemplate.opsForZSet().add(QUEUE_KEY, value, score);

        log.debug("Redis 큐 추가: messageId={}, scheduledAt={}", messageId, releaseTime);
    }

    /**
     * 큐 기반 지연(초) 적용
     */
    public void addToQueue(Long messageId, LocalDateTime scheduledAt, long delaySeconds) {
        LocalDateTime releaseTime = scheduledAt != null ? scheduledAt : LocalDateTime.now();
        if (delaySeconds > 0) {
            releaseTime = releaseTime.plusSeconds(delaySeconds);
        }
        long score = releaseTime.atZone(ZoneId.systemDefault()).toEpochSecond();

        String value = String.valueOf(messageId);
        redisTemplate.opsForZSet().add(QUEUE_KEY, value, score);

        log.debug("Redis 큐 추가(delay={}s): messageId={}, scheduledAt={}",
                delaySeconds, messageId, releaseTime);
    }

    /**
     * 발송 가능한 메시지 ID 조회 (현재 시간 이전)
     * @param limit 최대 조회 건수
     * @return 발송 가능한 메시지 ID 목록
     */
    public Set<String> getReadyMessageIds(int limit) {
        long now = System.currentTimeMillis() / 1000;
        return redisTemplate.opsForZSet().rangeByScore(QUEUE_KEY, 0, now, 0, limit);
    }

    /**
     * ZPOPMIN으로 원자적으로 가져온 뒤, 현재 시간 이전만 반환
     * - 미래 스코어는 다시 큐에 복원
     */
    public List<String> popReadyMessageIds(int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }

        long now = System.currentTimeMillis() / 1000;
        Set<ZSetOperations.TypedTuple<String>> popped =
                redisTemplate.opsForZSet().popMin(QUEUE_KEY, limit);

        if (popped == null || popped.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> ready = new ArrayList<>();
        List<ZSetOperations.TypedTuple<String>> future = new ArrayList<>();

        for (ZSetOperations.TypedTuple<String> tuple : popped) {
            if (tuple == null || tuple.getValue() == null || tuple.getScore() == null) {
                continue;
            }
            if (tuple.getScore() <= now) {
                ready.add(tuple.getValue());
            } else {
                future.add(tuple);
            }
        }

        if (!future.isEmpty()) {
            for (ZSetOperations.TypedTuple<String> tuple : future) {
                redisTemplate.opsForZSet().add(QUEUE_KEY, tuple.getValue(), tuple.getScore());
            }
        }

        return ready;
    }

    /**
     * 대기열에서 메시지 제거
     */
    public void removeFromQueue(Long messageId) {
        redisTemplate.opsForZSet().remove(QUEUE_KEY, String.valueOf(messageId));
    }

    /**
     * 대기열에서 메시지 제거 (String)
     */
    public void removeFromQueue(String messageId) {
        redisTemplate.opsForZSet().remove(QUEUE_KEY, messageId);
    }

    /**
     * 대기열 전체 크기
     */
    public Long getQueueSize() {
        return redisTemplate.opsForZSet().size(QUEUE_KEY);
    }

    /**
     * 발송 가능한 메시지 수 (현재 시간 이전)
     */
    public Long getReadyCount() {
        long now = System.currentTimeMillis() / 1000;
        return redisTemplate.opsForZSet().count(QUEUE_KEY, 0, now);
    }

    /**
     * 대기열 초기화
     */
    public void clearQueue() {
        redisTemplate.delete(QUEUE_KEY);
        log.info("Redis 큐 초기화 완료: key={}", QUEUE_KEY);
    }
}
