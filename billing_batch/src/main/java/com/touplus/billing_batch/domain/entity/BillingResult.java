package com.touplus.billing_batch.domain.entity;

import com.touplus.billing_batch.domain.enums.SendStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "billing_result",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_billing_month_user",
                        columnNames = {"settlement_month", "user_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 엔티티용 기본 생성자
@AllArgsConstructor // @Builder 사용을 위한 전체 필드 생성자
@Builder // BillingItemWriter에서 .builder()를 사용할 수 있게 함
public class BillingResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "billing_result_id")
    private Long id;

    @Column(name = "settlement_month", nullable = false)
    private LocalDate settlementMonth;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "total_price", nullable = false)
    private Integer totalPrice;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "settlement_details", columnDefinition = "json", nullable = false)
    private String settlementDetails;

    @Enumerated(EnumType.STRING)
    @Column(name = "send_status")
    @Builder.Default // 빌더 호출 시 값을 지정하지 않아도 READY가 기본값으로 들어가게 함
    private SendStatus sendStatus = SendStatus.READY;

    @Column(name = "batch_execution_id", nullable = false)
    private Long batchExecutionId;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    /* ===== 상태 변경 메서드 ===== */

    public void markSending() {
        this.sendStatus = SendStatus.SENDING;
    }

    public void markSuccess() {
        this.sendStatus = SendStatus.SUCCESS;
    }

    public void markFail() {
        // 필요 시 주석 해제 후 구현
        // this.sendStatus = SendStatus.FAIL;
    }
}