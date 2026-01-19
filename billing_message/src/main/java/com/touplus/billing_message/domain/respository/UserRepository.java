package com.touplus.billing_message.domain.respository;

import com.touplus.billing_message.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * User Repository
 * 유저 정보 조회용
 */
public interface UserRepository extends JpaRepository<User, Long> {
}
