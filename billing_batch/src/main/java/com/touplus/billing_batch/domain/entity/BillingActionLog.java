package com.touplus.billing_batch.domain.entity;

import com.touplus.billing_batch.domain.enums.*;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingActionLog {

    @Id
    @Column(name = "action_log_id", nullable = false)
    private Long actionLogId;

    @Column(name = "error_log_id")
    private Long errorLogId;

    @Column(name = "actor_type")
    private ActorType actorType;

    @Column(name = "actor_id")
    private String actorId;

    @Column(name = "action_type")
    private ActionType actionType;

    @Column(name = "action_message")
    private String actionMessage;

    @Column(name = "action_result")
    private ActionResult actionResult;

    @Column(name = "action_at")
    private LocalDateTime actionAt;
}