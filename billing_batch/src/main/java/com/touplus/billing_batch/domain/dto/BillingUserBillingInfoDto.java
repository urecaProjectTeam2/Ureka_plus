package com.touplus.billing_batch.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BillingUserBillingInfoDto {

    private Long userId;

    private List<UserSubscribeProductDto> products;
    private List<UnpaidDto> unpaids;
    private List<AdditionalChargeDto> additionalCharges;
    private List<UserSubscribeDiscountDto> discounts;
}