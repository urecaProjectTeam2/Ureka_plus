package com.touplus.billing_api.admin.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserDto {
    private Long userId;
    private String name;
    private String phone;
    private String email;
}
