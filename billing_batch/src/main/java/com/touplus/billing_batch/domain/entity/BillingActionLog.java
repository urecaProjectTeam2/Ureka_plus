package com.touplus.billing_batch.domain.entity;

import com.touplus.billing_batch.domain.enums.*;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "batch_billing_action_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class BillingActionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long actionLogId;

    private Long errorLogId;

    @Enumerated(EnumType.STRING)
    private ActorType actorType;

    private String actorId;

    @Enumerated(EnumType.STRING)
    private ActionType actionType;

    private String actionMessage;

    @Enumerated(EnumType.STRING)
    private ActionResult actionResult;

    @Builder.Default
    private LocalDateTime actionAt = LocalDateTime.now();
}