package com.touplus.billing_api.admin.service;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import java.util.List;

public interface BillingLogService {
    List<String> getLogFileList();
    ResponseEntity<Resource> downloadLogFile(String fileName);
    List<String> readLogFile(String fileName);
    boolean checkErrorInFile(String fileName);
}