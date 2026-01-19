package com.touplus.billing_batch.jobs.billing.step.reader;

import java.util.*;
import java.util.stream.Collectors;

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

    private Long lastUserId = 0L; // No-Offset 페이징용
    private StepExecution stepExecution;

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
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        // 재시작 시 lastUserId 복원
        if (executionContext.containsKey("lastUserId")) {
            this.lastUserId = executionContext.getLong("lastUserId");
        }
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        // 주기적으로 ExecutionContext에 진행 상태 저장
        executionContext.putLong("lastUserId", lastUserId);
    }

    @Override
    public void close() throws ItemStreamException {
        // 특별히 할 일 없음
    }

    @Override
    public BillingUserBillingInfoDto read() throws Exception {
        if (buffer.isEmpty()) {
            fillBuffer();
        }
        return buffer.poll();
    }

    private void fillBuffer() {
        // 1. No-Offset 방식: userId 기준 다음 청크 조회
        List<BillingUser> users = userRepository.findUsersGreaterThanId(lastUserId, Pageable.ofSize(chunkSize));

        if (users.isEmpty()) return;

        // 2. 조회한 마지막 ID 저장
        lastUserId = users.get(users.size() - 1).getUserId();

        List<Long> userIds = users.stream().map(BillingUser::getUserId).toList();

        // 3. 청크 단위로 각 테이블 벌크 조회
        Map<Long, List<UserSubscribeProduct>> uspMap = uspRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.groupingBy(p -> p.getUser().getUserId()));

        Map<Long, List<Unpaid>> unpaidMap = unpaidRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.groupingBy(u -> u.getUser().getUserId()));

        Map<Long, List<AdditionalCharge>> chargeMap = chargeRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.groupingBy(c -> c.getUser().getUserId()));

        Map<Long, List<UserSubscribeDiscount>> discountMap = discountRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.groupingBy(d -> d.getBillingUser().getUserId()));

        // 4. DTO 조립 후 버퍼에 추가
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
