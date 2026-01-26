package com.touplus.billing_api.admin.repository;

import com.touplus.billing_api.admin.dto.BatchBillingErrorLogDto;
import com.touplus.billing_api.admin.dto.BatchHistoryDto;
import com.touplus.billing_api.admin.dto.PartitionStatusDto;

import java.util.List;
import java.util.Map;

public interface JdbcBatchRepository {
    List<BatchHistoryDto> findAllHistory();
    Map<String, Object> findLatestSuccessfulBatch();
    List<PartitionStatusDto> findPartitionDetails(Long executionId);
    Long findLatestActiveExecutionId();
    List<BatchBillingErrorLogDto> findAllErrorLogsByJobId(Long executionId);

    List<BatchBillingErrorLogDto> findErrorLogsByJobIdAndUserId(Long jobId, Long userId);
}
