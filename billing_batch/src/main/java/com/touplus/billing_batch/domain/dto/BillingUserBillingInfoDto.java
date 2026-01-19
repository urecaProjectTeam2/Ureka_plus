package com.touplus.billing_batch.domain.dto;

import com.touplus.billing_batch.domain.entity.AdditionalCharge;
import com.touplus.billing_batch.domain.entity.Unpaid;
import com.touplus.billing_batch.domain.entity.UserSubscribeDiscount;
import com.touplus.billing_batch.domain.entity.UserSubscribeProduct;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@AllArgsConstructor
@Setter
@NoArgsConstructor
public class BillingUserBillingInfoDto {
    private Long userId;
    private List<UserSubscribeProduct> products;
    private List<Unpaid> unpaids;
    private List<AdditionalCharge> additionalCharges;
    private List<UserSubscribeDiscount> discounts;
}
