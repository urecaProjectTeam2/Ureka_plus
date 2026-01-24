package com.touplus.billing_batch.domain.entity;

import com.touplus.billing_batch.domain.enums.UseType;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import lombok.*;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUsage {

    @Id
    @Column(name = "user_usage_id", nullable = false)
    private Long userUsageId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "use_month", nullable = false)
    private LocalDate useMonth;

    @Column(name = "use_type", nullable = false)
    private UseType useType;

    @Column(name = "use_amount")
    private Integer useAmount;
}
