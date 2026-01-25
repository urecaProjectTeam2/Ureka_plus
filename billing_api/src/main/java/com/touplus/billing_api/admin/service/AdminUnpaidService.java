package com.touplus.billing_api.admin.service;

import com.touplus.billing_api.admin.dto.PageResponseDto;
import com.touplus.billing_api.admin.dto.UnpaidUserResponse;

import java.time.LocalDate;
import java.util.List;

public interface AdminUnpaidService {

    PageResponseDto<UnpaidUserResponse> getUnpaidUsers(int page, int size);
//    List<UnpaidUserResponse> getUnpaidUsersByMonth(int page, int size, String month);

    PageResponseDto<UnpaidUserResponse> searchUnpaidUsersByKeyword(int page, int size, String keyword);
}
