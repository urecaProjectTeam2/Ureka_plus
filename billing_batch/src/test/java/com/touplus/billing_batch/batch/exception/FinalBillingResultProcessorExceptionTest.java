package com.touplus.billing_batch.batch.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.touplus.billing_batch.common.BillingException;
import com.touplus.billing_batch.common.BillingFatalException;
import com.touplus.billing_batch.domain.dto.BillingUserBillingInfoDto;
import com.touplus.billing_batch.domain.dto.BillingWorkDto;
import com.touplus.billing_batch.jobs.billing.step.processor.FinalBillingResultProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class FinalBillingResultProcessorExceptionTest {

    @InjectMocks
    private FinalBillingResultProcessor processor;

    @Mock
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // @Value 필드 수동 주입
        ReflectionTestUtils.setField(processor, "jobExecutionId", 123L);
        ReflectionTestUtils.setField(processor, "targetMonth", "2026-01-01");
    }

    @Test
    @DisplayName("1. 미납 월 정보(unpaidMonth)가 누락되면 BillingException 발생")
    void process_MissingUnpaidMonth_ThrowsException() {
        // Given: 미납 정보는 있으나 월 정보가 null인 경우
        UnpaidDto unpaid = UnpaidDto.builder().unpaidId(1L).unpaidPrice(5000).unpaidMonth(null).build();
        BillingWorkDto work = createBaseWork(1659L, 10000);
        work.getRawData().getUnpaids().add(unpaid);

        // When & Then
        assertThatThrownBy(() -> processor.process(work))
                .isExactlyInstanceOf(BillingException.class)
                .hasMessageContaining("미납 월이 비어있습니다");
    }

    @Test
    @DisplayName("2. 미납 금액이 음수이면 invalidUnpaidAmount(BillingException) 발생")
    void process_NegativeUnpaidPrice_ThrowsException() {
        // Given: 미납 금액이 -500원인 경우
        UnpaidDto unpaid = UnpaidDto.builder().unpaidId(1L).unpaidPrice(-500).unpaidMonth(LocalDate.parse("2025-12-01")).build();
        BillingWorkDto work = createBaseWork(1659L, 10000);
        work.getRawData().getUnpaids().add(unpaid);

        // When & Then
        assertThatThrownBy(() -> processor.process(work))
                .isExactlyInstanceOf(BillingException.class)
                .hasFieldOrPropertyWithValue("errorCode", "ERR_INVALID_UNPAID_DATA");
    }

    @Test
    @DisplayName("3. 최종 합산 금액이 Integer 범위를 초과하면 BillingFatalException 발생")
    void process_FinalPriceOverflow_ThrowsFatalException() {
        // Given: 기본 금액이 MAX_VALUE인 상태에서 미납금이 추가되는 경우
        UnpaidDto unpaid = UnpaidDto.builder().unpaidId(1L).unpaidPrice(100).unpaidMonth(LocalDate.parse("2025-12-01")).build();
        BillingWorkDto work = createBaseWork(1659L, Integer.MAX_VALUE);
        work.getRawData().getUnpaids().add(unpaid);

        // When & Then
        assertThatThrownBy(() -> processor.process(work))
                .isExactlyInstanceOf(BillingFatalException.class)
                .hasFieldOrPropertyWithValue("errorCode", "FATAL_INVALID_AMOUNT");
    }

    @Test
    @DisplayName("4. JSON 변환(ObjectMapper) 중 에러 발생 시 예외 전파 확인")
    void process_JsonSerializationError_ThrowsException() throws Exception {
        // Given: ObjectMapper가 에러를 던지도록 설정
        given(objectMapper.writeValueAsString(any())).willThrow(new RuntimeException("JSON Error"));
        BillingWorkDto work = createBaseWork(1659L, 10000);

        // When & Then
        assertThatThrownBy(() -> processor.process(work))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("JSON Error");
    }

    // --- Helper Method ---
    private BillingWorkDto createBaseWork(Long userId, int totalPrice) {
        BillingUserBillingInfoDto rawData = BillingUserBillingInfoDto.builder()
                .userId(userId)
                .unpaids(new ArrayList<>())
                .build();

        return BillingWorkDto.builder()
                .rawData(rawData)
                .totalPrice(totalPrice)
                .mobile(new ArrayList<>())
                .internet(new ArrayList<>())
                .iptv(new ArrayList<>())
                .dps(new ArrayList<>())
                .addon(new ArrayList<>())
                .discounts(new ArrayList<>())
                .unpaids(new ArrayList<>())
                .build();
    }
}
