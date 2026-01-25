package com.touplus.billing_message.service;

import lombok.extern.slf4j.Slf4j;

/**
 * [비활성화] Redis ZSet 기반으로 전환됨
 *
 * 기존 역할:
 * - DB에서 메시지 조회 → 스냅샷 생성 → DelayQueue에 투입
 *
 * 새 구조:
 * - MessageProcessor: INSERT 후 바로 Redis 큐에 추가
 * - MessageDispatchScheduler: Redis 폴링 → 직접 발송 처리
 */
// @Service  // 비활성화
@Slf4j
public class MessageDispatchService {

    // Redis 기반 구조에서는 이 서비스가 불필요합니다.
    // 모든 로직이 MessageDispatchScheduler로 이동되었습니다.

}
