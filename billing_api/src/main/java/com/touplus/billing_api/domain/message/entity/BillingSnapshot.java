package com.touplus.billing_api.domain.message.entity;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import lombok.Getter;

import java.time.LocalDate;

/**
 * billing_snapshot 테이블 Row 매핑용 객체 (JDBC)
 */
@Getter
@AllArgsConstructor
public class BillingSnapshot {

    @Id
    @Column(name = "billing_id")
    private final Long billingId;

    @Column(name = "settlement_month")
    private final LocalDate settlementMonth;

    @Column(name = "user_id")
    private final Long userId;

    @Column(name = "total_price")
    private final Integer totalPrice;

    /**
     * JSON 컬럼
     * - DB에서는 json
     * - JDBC에서는 String으로 받는 것을 권장
     */
    @Column(name = "settlement_details")
    private final String settlementDetails;

}