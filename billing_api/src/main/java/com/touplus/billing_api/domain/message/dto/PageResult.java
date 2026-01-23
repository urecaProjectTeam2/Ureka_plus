package com.touplus.billing_api.domain.message.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PageResult<T> {
    private List<T> content; // 현재 페이지 데이터
    private int totalPages;  // 총 페이지 수
    private long totalElements; // 총 데이터 수
    private int page;        // 현재 페이지 번호
}
