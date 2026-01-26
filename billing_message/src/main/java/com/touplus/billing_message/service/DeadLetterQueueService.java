package com.touplus.billing_message.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dead Letter Queue 서비스
 * - 처리 실패한 메시지를 별도 보관
 * - 일정 시간 후 자동 재처리 또는 수동 재처리 지원
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeadLetterQueueService {

    private final StringRedisTemplate redisTemplate;
    private final WaitingQueueService waitingQueueService;

    private static final String DLQ_KEY = "queue:message:dead-letter";
    private static final int MAX_RETRY_COUNT = 5;
    private static final RedisScript<List> POP_SCRIPT;

    static {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setResultType(List.class);
        script.setScriptText(
                "local key = KEYS[1]\n"
                        + "local limit = tonumber(ARGV[1])\n"
                        + "if limit <= 0 then return {} end\n"
                        + "local ids = redis.call('ZRANGE', key, 0, limit - 1)\n"
                        + "if #ids > 0 then\n"
                        + "  redis.call('ZREM', key, unpack(ids))\n"
                        + "end\n"
                        + "return ids\n");
        POP_SCRIPT = script;
    }

    /**
     * DLQ에 단일 메시지 추가
     */
    public void add(Long messageId, String reason) {
        long score = System.currentTimeMillis() / 1000;
        String value = messageId + ":" + reason + ":1"; // messageId:reason:retryCount
        redisTemplate.opsForZSet().add(DLQ_KEY, value, score);
        log.warn("DLQ 추가: messageId={}, reason={}", messageId, reason);
    }

    /**
     * DLQ에 메시지 일괄 추가 (Pipeline)
     */
    public void addBatch(List<Long> messageIds, String reason) {
        if (messageIds == null || messageIds.isEmpty()) {
            return;
        }

        long score = System.currentTimeMillis() / 1000;
        byte[] keyBytes = DLQ_KEY.getBytes();

        redisTemplate.executePipelined(new RedisCallback<Object>() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                for (Long messageId : messageIds) {
                    String value = messageId + ":" + reason + ":1";
                    connection.zSetCommands().zAdd(keyBytes, score, value.getBytes());
                }
                return null;
            }
        });

        log.warn("DLQ 일괄 추가: {}건, reason={}", messageIds.size(), reason);
    }

    /**
     * DLQ에서 메시지 꺼내서 원래 큐로 이동 (재처리)
     * @return 재처리된 건수
     */
    public int retryFromDeadLetter(int limit) {
        List<String> entries = redisTemplate.execute(
                POP_SCRIPT,
                Collections.singletonList(DLQ_KEY),
                String.valueOf(limit));

        if (entries == null || entries.isEmpty()) {
            return 0;
        }

        Map<Long, LocalDateTime> retryMap = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();

        for (String entry : entries) {
            try {
                String[] parts = entry.split(":");
                Long messageId = Long.parseLong(parts[0]);
                int retryCount = parts.length >= 3 ? Integer.parseInt(parts[2]) : 1;

                if (retryCount >= MAX_RETRY_COUNT) {
                    // 최대 재시도 초과 - 영구 DLQ로 이동
                    addToPermanentDlq(messageId, parts.length >= 2 ? parts[1] : "UNKNOWN");
                    log.error("최대 재시도 초과 → 영구 DLQ: messageId={}", messageId);
                    continue;
                }

                retryMap.put(messageId, now);
            } catch (Exception e) {
                log.error("DLQ 파싱 실패: entry={}", entry, e);
            }
        }

        if (!retryMap.isEmpty()) {
            waitingQueueService.addToQueueBatch(retryMap, 0);
            log.info("DLQ → 메인 큐 재처리: {}건", retryMap.size());
        }

        return retryMap.size();
    }

    /**
     * 영구 DLQ에 추가 (최대 재시도 초과)
     */
    private void addToPermanentDlq(Long messageId, String reason) {
        long score = System.currentTimeMillis() / 1000;
        String value = messageId + ":" + reason;
        redisTemplate.opsForZSet().add("queue:message:dead-letter-permanent", value, score);
    }

    /**
     * DLQ 전체를 메인 큐로 이동 (수동 재처리)
     */
    public int retryAll() {
        Long count = getCount();
        if (count == null || count == 0) {
            return 0;
        }
        return retryFromDeadLetter(count.intValue());
    }

    /**
     * DLQ 현재 건수
     */
    public Long getCount() {
        return redisTemplate.opsForZSet().size(DLQ_KEY);
    }

    /**
     * 영구 DLQ 현재 건수
     */
    public Long getPermanentCount() {
        return redisTemplate.opsForZSet().size("queue:message:dead-letter-permanent");
    }

    /**
     * DLQ 초기화 (주의: 모든 실패 메시지 삭제)
     */
    public void clear() {
        redisTemplate.delete(DLQ_KEY);
        log.warn("DLQ 초기화 완료");
    }
}
