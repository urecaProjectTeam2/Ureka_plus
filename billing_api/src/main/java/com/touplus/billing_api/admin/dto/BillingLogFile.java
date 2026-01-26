package com.touplus.billing_api.admin.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
public class BillingLogFile {
    // 1. 필드(변수) 선언이 반드시 필요합니다.
    private String fileName;
    private LocalDateTime dateTime;
    private boolean hasError;

    // 2. 클래스 이름과 동일한 생성자입니다.
    public BillingLogFile(String fileName, boolean hasError) {
        this.fileName = fileName;
        this.hasError = hasError;

        try {
            if (fileName != null && fileName.contains("_")) {
                String[] parts = fileName.split("_");
                // billing_exec_20260126_0042.log 형식 체크
                if (parts.length >= 4) {
                    String datePart = parts[2]; // 20260126
                    String timePart = parts[3].replace(".log", ""); // 0042

                    // 시간 파트가 4자리가 아닐 경우를 대비 (예: 42 -> 0042)
                    if(timePart.length() < 4) {
                        try {
                            timePart = String.format("%04d", Integer.parseInt(timePart));
                        } catch (NumberFormatException e) {
                            timePart = "0000";
                        }
                    }

                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
                    this.dateTime = LocalDateTime.parse(datePart + timePart, formatter);
                } else {
                    this.dateTime = LocalDateTime.now();
                }
            } else {
                this.dateTime = LocalDateTime.now();
            }
        } catch (Exception e) {
            System.out.println("로그 파일명 파싱 실패: " + fileName);
            this.dateTime = LocalDateTime.now(); // 실패 시 현재 시간으로 세팅하여 에러 방지
        }
    }
}