package com.touplus.billing_batch.jobs.billing.step.reader;

import java.util.*;
import java.util.stream.Collectors;

import com.touplus.billing_batch.common.BillingException;
import com.touplus.billing_batch.common.BillingFatalException;
import com.touplus.billing_batch.domain.dto.*;
import com.touplus.billing_batch.jobs.billing.step.listener.BillingErrorLogger;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.touplus.billing_batch.domain.entity.*;
import com.touplus.billing_batch.domain.repository.*;

@Component
@StepScope
public class BillingItemReader implements ItemStreamReader<BillingUserBillingInfoDto> {

    private final BillingUserRepository userRepository;
    private final UserSubscribeProductRepository uspRepository;
    private final UnpaidRepository unpaidRepository;
    private final AdditionalChargeRepository chargeRepository;
    private final UserSubscribeDiscountRepository discountRepository;

    private final Deque<BillingUserBillingInfoDto> buffer = new ArrayDeque<>();
    private final int chunkSize = 1000;

    @Value("#{stepExecutionContext['minValue']}")
    private Long minValue;

    @Value("#{stepExecutionContext['maxValue']}")
    private Long maxValue;

    // DB 조회용
    private Long lastProcessedUserId = 0L;  // 실제 read()로 반환된 ID
    private StepExecution stepExecution;
    private static final String CTX_LAST_PROCESSED_USER_ID = "lastProcessedUserId";

    public BillingItemReader(
            BillingUserRepository userRepository,
            UserSubscribeProductRepository uspRepository,
            UnpaidRepository unpaidRepository,
            AdditionalChargeRepository chargeRepository,
            UserSubscribeDiscountRepository discountRepository
    ) {
        this.userRepository = userRepository;
        this.uspRepository = uspRepository;
        this.unpaidRepository = unpaidRepository;
        this.chargeRepository = chargeRepository;
        this.discountRepository = discountRepository;
    }

    // StepExecution 주입 (ExecutionContext 접근용)
    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        this.stepExecution = stepExecution;
    }

    @Override
    public void open(ExecutionContext executionContext) {
        if (minValue == null || maxValue == null) {
            throw BillingFatalException.cacheNotFound("Partition minValue / maxValue not set");
        }

        if (executionContext.containsKey(CTX_LAST_PROCESSED_USER_ID)) {
            this.lastProcessedUserId =
                    executionContext.getLong(CTX_LAST_PROCESSED_USER_ID);
        } else {
            // 파티션 시작은 minValue부터
            this.lastProcessedUserId = minValue - 1;
        }
    }


    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        executionContext.putLong(CTX_LAST_PROCESSED_USER_ID, lastProcessedUserId);
    }

    @Override
    public void close() throws ItemStreamException {
        // 특별히 할 일 없음
    }

    @Override
    public BillingUserBillingInfoDto read() throws Exception {
        //  버퍼가 비었으면 채우기 (조회 실패 시 Fatal)
        if (buffer.isEmpty()) {
            fillBuffer();
        }

        BillingUserBillingInfoDto dto = buffer.poll();

        // 개별 DTO 검증: 상품 정보가 없으면 Skip 가능
        if (dto != null) {
            if (true) {
//            if (dto.getProducts() == null || dto.getProducts().isEmpty()) {
                throw new BillingException("상품 정보 없음", "ERR_NO_PRODUCT", dto.getUserId());
            }
            lastProcessedUserId = dto.getUserId();
        }

        return dto;
    }


    private void fillBuffer() throws Exception {
        // 1. No-Offset 방식: userId 기준 다음 청크 조회
        List<BillingUser> users =
                userRepository.findUsersInRange(
                        minValue,
                        maxValue,
                        lastProcessedUserId,
                        Pageable.ofSize(chunkSize)
                );

        if (users.isEmpty()) {
            throw BillingFatalException.noUsersInPartition(minValue, maxValue);
        }

        List<Long> userIds = users.stream().map(BillingUser::getUserId).toList();

        // 엔티티 -> DTO 변환 + Map 생성
        Map<Long, List<UserSubscribeProductDto>> uspMap = uspRepository.findByUserIdIn(userIds).stream()
                .map(UserSubscribeProductDto::fromEntity)
                .collect(Collectors.groupingBy(UserSubscribeProductDto::getUserId));

        Map<Long, List<UnpaidDto>> unpaidMap = unpaidRepository.findByUserIdIn(userIds).stream()
                .map(UnpaidDto::fromEntity)
                .collect(Collectors.groupingBy(UnpaidDto::getUserId));

        Map<Long, List<AdditionalChargeDto>> chargeMap = chargeRepository.findByUserIdIn(userIds).stream()
                .map(AdditionalChargeDto::fromEntity)
                .collect(Collectors.groupingBy(AdditionalChargeDto::getUserId));

        Map<Long, List<UserSubscribeDiscountDto>> discountMap = discountRepository.findByUserIdIn(userIds).stream()
                .map(UserSubscribeDiscountDto::fromEntity)
                .collect(Collectors.groupingBy(UserSubscribeDiscountDto::getUserId));

        // 3. DTO 조립 후 버퍼에 추가
        for (BillingUser user : users) {
            BillingUserBillingInfoDto dto = new BillingUserBillingInfoDto(
                    user.getUserId(),
                    uspMap.getOrDefault(user.getUserId(), List.of()),
                    unpaidMap.getOrDefault(user.getUserId(), List.of()),
                    chargeMap.getOrDefault(user.getUserId(), List.of()),
                    discountMap.getOrDefault(user.getUserId(), List.of())
            );
            buffer.add(dto);
        }
    }
}