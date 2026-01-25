package com.touplus.billing_api.admin.dto;

import com.touplus.billing_api.domain.billing.enums.ProductType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingProductStatResponse {
    private Long productId;           
    private String productName;       
    private ProductType productType;  
    private Integer price;            
    private Long subscribeCount;      
}
