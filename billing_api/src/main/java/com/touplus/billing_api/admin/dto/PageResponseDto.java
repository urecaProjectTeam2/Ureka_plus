package com.touplus.billing_api.admin.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PageResponseDto<T> {

    private List<T> contents;   // 현재 페이지의 데이터 목록
    private int page;           // 현재 페이지 (0-based)
    private int size;           // 페이지 크기
    private long totalElements; // 전체 데이터 수
    private int totalPages;     // 전체 페이지 수
}

