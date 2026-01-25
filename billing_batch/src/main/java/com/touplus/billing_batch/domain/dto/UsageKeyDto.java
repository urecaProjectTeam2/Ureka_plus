package com.touplus.billing_batch.domain.dto;

import com.touplus.billing_batch.domain.enums.UseType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class UsageKeyDto {
    private Long productId;
    private UseType useType;
}