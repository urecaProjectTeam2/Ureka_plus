package com.touplus.billing_message.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Set;

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
    private static final RedisScript<List> POP_READY_SCRIPT;

    static {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setResultType(List.class);
        script.setScriptText(
                "local key = KEYS[1]\n"
                        + "local maxScore = ARGV[1]\n"
                        + "local limit = tonumber(ARGV[2])\n"
                        + "if limit <= 0 then return {} end\n"
                        + "local ids = redis.call('ZRANGEBYSCORE', key, '-inf', maxScore, 'LIMIT', 0, limit)\n"
                        + "if #ids > 0 then\n"
                        + "  redis.call('ZREM', key, unpack(ids))\n"
                        + "end\n"
                        + "return ids\n");
        POP_READY_SCRIPT = script;
    }

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
     * - Lua로 현재 시간 이전만 원자적으로 pop
     */
    public List<String> popReadyMessageIds(int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }

        long now = System.currentTimeMillis() / 1000;
        List<String> result = redisTemplate.execute(
                POP_READY_SCRIPT,
                Collections.singletonList(QUEUE_KEY),
                String.valueOf(now),
                String.valueOf(limit));

        return result != null ? result : Collections.emptyList();
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
