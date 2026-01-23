package com.touplus.billing_api.domain.repository.message;

import com.touplus.billing_api.domain.message.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

    /**
     * user_id로 단일 유저 조회
     */
    Optional<User> findById(Long userId);

    /**
     * user_id 목록으로 유저 조회 (대량 처리 대비)
     */
    List<User> findByIds(List<Long> userIds);

    List<User> findAllPaged(int offset, int limit);
    long countAll();
}