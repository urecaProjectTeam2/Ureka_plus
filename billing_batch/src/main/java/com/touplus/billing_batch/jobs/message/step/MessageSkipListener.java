package com.touplus.billing_batch.jobs.message.step;

import com.touplus.billing_batch.domain.dto.BillingResultDto;
import com.touplus.billing_batch.domain.enums.ErrorType;
import com.touplus.billing_batch.jobs.billing.step.listener.BillingErrorLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageSkipListener implements SkipListener<BillingResultDto, BillingResultDto> {

    private final JdbcTemplate jdbcTemplate;
    private final BillingErrorLogger billingErrorLogger;

    @Override
    public void onSkipInRead(Throwable t) {
        log.error("메시지 읽기 중 스킵 발생: {}", t.getMessage());
    }

    @Override
    public void onSkipInProcess(BillingResultDto item, Throwable t) {
        log.error("메시지 프로세싱 중 스킵 발생 (User ID: {}): {}", item.getUserId(), t.getMessage());
        handleMessageError(item.getUserId(), t, "PROCESSOR");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onSkipInWrite(BillingResultDto item, Throwable t) {
        log.error("메시지 전송 실패 (User ID: {}): {}", item.getUserId(), t.getMessage());

        try {
            // 1. 공통 로거 호출 (NETWORK 타입 강제)
            handleMessageError(item.getUserId(), t, "SENDER");

            // 2. 메시지 상태 업데이트
            updateStatusToFail(item.getId());
        } catch (Exception e) {
            log.error("에러 핸들링 중 치명적 오류 발생: {}", e.getMessage());
        }
    }

    private void handleMessageError(Long userId, Throwable t, String phase) {
        StepExecution stepExecution = Optional.ofNullable(StepSynchronizationManager.getContext())
                .map(StepContext::getStepExecution)
                .orElse(null);

        if (stepExecution != null) {
            // [MODIFIED] ErrorType.NETWORK를 명시적으로 전달하여 Billing 로직과 분리
            billingErrorLogger.saveErrorLog(stepExecution, userId, t, "MSG_" + phase, false, ErrorType.NETWORK);
        }
    }

    private void updateStatusToFail(Long id) {
        String sql = "UPDATE billing_result SET send_status = 'FAIL', processed_at = NOW() WHERE billing_result_id = ?";
        jdbcTemplate.update(sql, id);
    }
}