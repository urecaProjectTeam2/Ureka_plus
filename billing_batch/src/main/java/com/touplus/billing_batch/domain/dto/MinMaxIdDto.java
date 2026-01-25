package com.touplus.billing_batch.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MinMaxIdDto {
    private Long minId;
    private Long maxId;
}
