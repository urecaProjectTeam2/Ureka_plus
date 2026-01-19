package com.touplus.billing_message.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * users 테이블 Entity
 * 유저 정보 조회용 (발송 시간 계산에 사용)
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor
public class User {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "name", nullable = false, length = 30)
    private String name;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "phone", nullable = false)
    private String phone;

    @Column(name = "sending_day", nullable = false)
    private Integer sendingDay;  // 발송 예정일 (1~28)

    @Column(name = "ban_start_time")
    private LocalTime banStartTime;  // 발송 금지 시작 시간

    @Column(name = "ban_end_time")
    private LocalTime banEndTime;    // 발송 금지 종료 시간

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    private MessageType messageType;  // SMS or EMAIL
}
