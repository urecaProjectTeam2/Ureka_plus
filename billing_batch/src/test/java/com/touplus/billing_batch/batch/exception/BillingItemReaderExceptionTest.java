package com.touplus.billing_batch.batch.exception;

import com.touplus.billing_batch.common.BillingFatalException;
import com.touplus.billing_batch.domain.entity.BillingUser;
import com.touplus.billing_batch.domain.repository.service.BillingUserRepository;
import com.touplus.billing_batch.domain.repository.service.UserSubscribeProductRepository;
import com.touplus.billing_batch.jobs.billing.step.reader.BillingItemReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class BillingItemReaderExceptionTest {

    @InjectMocks
    private BillingItemReader billingItemReader;

    @Mock
    private BillingUserRepository userRepository;

    @Mock
    private UserSubscribeProductRepository uspRepository;

    // 나머지 Repository들 Mock 생략 (필요 시 추가)

    @BeforeEach
    void setUp() {
        // @Value 필드 수동 주입
        ReflectionTestUtils.setField(billingItemReader, "targetMonth", "2026-01-01");
        ReflectionTestUtils.setField(billingItemReader, "minValue", 1L);
        ReflectionTestUtils.setField(billingItemReader, "maxValue", 10000L);
    }

    @Test
    @DisplayName("1. 날짜 형식이 잘못되면 open 단계에서 ERR_INVALID_DATE 발생")
    void open_InvalidDate_ThrowsException() {
        ReflectionTestUtils.setField(billingItemReader, "targetMonth", "invalid-date");
        ExecutionContext executionContext = new ExecutionContext();

        assertThatThrownBy(() -> billingItemReader.open(executionContext))
                .isInstanceOf(BillingFatalException.class)
                .hasFieldOrPropertyWithValue("errorCode", "ERR_INVALID_DATE");
    }

//    @Test
//    @DisplayName("2. 첫 번째 조회인데 유저가 없으면 DATA_NOT_FOUND 발생")
//    void fillBuffer_NoUsersOnFirstRead_ThrowsException() {
//        // Given
//        ExecutionContext executionContext = new ExecutionContext();
//        billingItemReader.open(executionContext); // lastProcessedUserId = 0L 세팅
//
//        given(userRepository.findUsersInRange(anyLong(), anyLong(), anyLong(), anyBoolean(), any(), any(), any()))
//                .willReturn(List.of()); // 유저 없음
//
//        // When & Then
//        assertThatThrownBy(() -> billingItemReader.read())
//                .isInstanceOf(BillingFatalException.class)
//                .hasMessageContaining("정산 대상 유저가 존재하지 않습니다");
//    }

    @Test
    @DisplayName("3. 유저는 있지만 구독 상품 정보가 전무하면 정합성 오류 발생")
    void fillBuffer_NoProductInfo_ThrowsException() {
        // Given
        ExecutionContext executionContext = new ExecutionContext();
        billingItemReader.open(executionContext);

        BillingUser mockUser = new BillingUser();
        ReflectionTestUtils.setField(mockUser, "userId", 100L);

        given(userRepository.findUsersInRange(anyLong(), anyLong(), anyLong(), anyBoolean(), any(), any(), any()))
                .willReturn(List.of(mockUser));

        given(uspRepository.findByUserIdIn(anyList(), any(), any()))
                .willReturn(new ArrayList<>()); // 상품 정보 없음

        // When & Then
        assertThatThrownBy(() -> billingItemReader.read())
                .isInstanceOf(BillingFatalException.class)
                .hasMessageContaining("구독 상품 정보가 존재하지 않습니다");
    }

    @Test
    @DisplayName("4. DB 조회 중 예외가 발생하면 ERR_READER_UNKNOWN으로 래핑")
    void read_RuntimeException_WrapsAsFatal() {
        // Given
        ExecutionContext executionContext = new ExecutionContext();
        billingItemReader.open(executionContext);

        given(userRepository.findUsersInRange(anyLong(), anyLong(), anyLong(), anyBoolean(), any(), any(), any()))
                .willThrow(new RuntimeException("DB Connection Timeout"));

        // When & Then
        assertThatThrownBy(() -> billingItemReader.read())
                .isInstanceOf(BillingFatalException.class)
                .hasFieldOrPropertyWithValue("errorCode", "ERR_READER_UNKNOWN");
    }

    @Test
    @DisplayName("5. Partition 범위(minValue/maxValue)가 누락되면 open 단계에서 ERR_NO_PARTITION 발생")
    void open_MissingPartitionValues_ThrowsException() {
        // Given: setUp에서 설정된 값을 강제로 null로 덮어씌움
        ReflectionTestUtils.setField(billingItemReader, "minValue", null);
        ReflectionTestUtils.setField(billingItemReader, "maxValue", null);
        ExecutionContext executionContext = new ExecutionContext();

        // When & Then
        assertThatThrownBy(() -> billingItemReader.open(executionContext))
                .isInstanceOf(BillingFatalException.class)
                .hasFieldOrPropertyWithValue("errorCode", "ERR_NO_PARTITION")
                .hasMessageContaining("설정되지 않았습니다");
    }
}