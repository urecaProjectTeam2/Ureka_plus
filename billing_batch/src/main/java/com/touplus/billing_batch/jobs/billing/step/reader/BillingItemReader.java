package com.touplus.billing_batch.jobs.billing.step.reader;

import java.util.*;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.touplus.billing_batch.domain.entity.*;
import com.touplus.billing_batch.domain.dto.BillingUserBillingInfoDto;
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

    @PersistenceContext
    private EntityManager entityManager;

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
        if (executionContext.containsKey(CTX_LAST_PROCESSED_USER_ID)) {
            this.lastProcessedUserId = executionContext.getLong(CTX_LAST_PROCESSED_USER_ID);

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
    public BillingUserBillingInfoDto read() {
        if (buffer.isEmpty()) {
            fillBuffer();
        }
        BillingUserBillingInfoDto dto = buffer.poll();
        if (dto != null) {
            lastProcessedUserId = dto.getUserId();
        }
        return dto;
    }

    private void fillBuffer() {
        // 1. No-Offset 방식: userId 기준 다음 청크 조회
        List<BillingUser> users = userRepository.findUsersGreaterThanId(lastProcessedUserId, Pageable.ofSize(chunkSize));

        if (users.isEmpty()) return;

        List<Long> userIds = users.stream().map(BillingUser::getUserId).toList();

        // 2. 청크 단위로 각 테이블 벌크 조회
        Map<Long, List<UserSubscribeProduct>> uspMap = uspRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.groupingBy(p -> p.getUser().getUserId()));

        Map<Long, List<Unpaid>> unpaidMap = unpaidRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.groupingBy(u -> u.getUser().getUserId()));

        Map<Long, List<AdditionalCharge>> chargeMap = chargeRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.groupingBy(c -> c.getUser().getUserId()));

        Map<Long, List<UserSubscribeDiscount>> discountMap = discountRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.groupingBy(d -> d.getBillingUser().getUserId()));

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