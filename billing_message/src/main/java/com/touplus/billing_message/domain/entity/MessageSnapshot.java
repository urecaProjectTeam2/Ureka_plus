package com.touplus.billing_message.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "message_snapshot")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MessageSnapshot {

    @Id
    @Column(name = "message_id")
    private Long messageId;

    @Column(name = "billing_id", nullable = false)
    private Long billingId;

    @Column(name = "settlement_month", nullable = false)
    private LocalDate settlementMonth;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_name", nullable = false)
    private String userName;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(name = "user_phone", nullable = false)
    private String userPhone;

    @Column(name = "total_price", nullable = false)
    private Integer totalPrice;

    @Column(name = "settlement_details", columnDefinition = "json", nullable = false)
    private String settlementDetails;

    @Column(name = "message_content", nullable = false)
    private String messageContent;
}
