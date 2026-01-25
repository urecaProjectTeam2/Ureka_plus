package com.touplus.billing_batch.jobs.billing.step.reader;

import com.touplus.billing_batch.common.BillingFatalException;
import com.touplus.billing_batch.domain.dto.*;
import com.touplus.billing_batch.domain.entity.BillingUser;
import com.touplus.billing_batch.domain.repository.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@StepScope
@Slf4j
public class BillingItemReader implements ItemStreamReader<BillingUserBillingInfoDto> {

    private final BillingUserRepository userRepository;
    private final UserSubscribeProductRepository uspRepository;
    private final AdditionalChargeRepository chargeRepository;
    private final UserSubscribeDiscountRepository discountRepository;
    private final UserUsageRepository userUsageRepository;
    private final GroupDiscountRepository groupDiscountRepository;

    public BillingItemReader(
            BillingUserRepository userRepository,
            UserSubscribeProductRepository uspRepository,
            AdditionalChargeRepository chargeRepository,
            UserSubscribeDiscountRepository discountRepository,
            UserUsageRepository userUsageRepository,
            GroupDiscountRepository groupDiscountRepository
    ) {
        this.userRepository = userRepository;
        this.uspRepository = uspRepository;
        this.chargeRepository = chargeRepository;
        this.discountRepository = discountRepository;
        this.userUsageRepository = userUsageRepository;
        this.groupDiscountRepository =groupDiscountRepository;
    }

    @Value("#{stepExecutionContext['minValue']}")
    private Long minValue;

    @Value("#{stepExecutionContext['maxValue']}")
    private Long maxValue;

    @Value("#{jobParameters['forceFullScan'] ?: false}")
    private boolean forceFullScan;

    @Value("#{jobParameters['targetMonth']}")
    private String targetMonth;

    @Value("#{jobParameters['chunkSize'] ?: 2000}")
    private int chunkSize;

    private LocalDate startDate;
    private LocalDate endDate;

    // groupId와 usage를 제공해야 함!

    private Long lastProcessedUserId = 0L;
    private List<BillingUserBillingInfoDto> buffer = new ArrayList<>();
    private int nextIndex = 0;
    private static final String CTX_LAST_PROCESSED_USER_ID = "lastProcessedUserId";

    @Override
    public void open(ExecutionContext executionContext) {
        if (forceFullScan) {
            log.info(">> [NOTICE] 전체 재정산 모드(forceFullScan=true)로 동작합니다. 모든 데이터를 다시 처리합니다.");
        }

        try {
            if(targetMonth != null){
                // targetMonth 시작일-종료일 계산
                this.startDate = LocalDate.parse(targetMonth);
                this.endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
            } else{
                throw new BillingFatalException("targetMonth가 존재하지 않습니다: ", "ERR_NO_DATE", 0L);
            }
        } catch (Exception e) {
            throw new BillingFatalException("targetMonth 형식이 올바르지 않습니다: " + targetMonth, "ERR_INVALID_DATE", 0L);
        }

        if (minValue == null || maxValue == null) {
            throw new BillingFatalException("Partition minValue / maxValue가 설정되지 않았습니다.", "ERR_NO_PARTITION", 0L);
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
    public BillingUserBillingInfoDto read() {
        // 청크 단위로 데이터 읽기 --> 베퍼에 저장 --> 하나씩 pop
        if (nextIndex >= buffer.size()) {
            fillBuffer();   // DB에서 fill
            nextIndex = 0;
            if (buffer.isEmpty()) return null;  // 배치 종료
        }
        return buffer.get(nextIndex++);
    }

    private void fillBuffer() {
        buffer.clear();

        // 1. 청구 대상 유저 선택 & groupMember 수 파악
        // '미정구 유저 찾기'
        List<BillingUserMemberDto> users = userRepository.findUsersInRange(
                minValue, maxValue, lastProcessedUserId, forceFullScan, startDate, endDate, Pageable.ofSize(chunkSize)
        );

        if (users.isEmpty()) return;

        // 2. Entity를 조회 후 DTO로 변환하여 Map으로 그룹화(Bulk 조회 준비동작)
        List<Long> userIds = users.stream().map(BillingUserMemberDto::getUserId).toList();

        // 2. Entity를 조회 후 DTO로 변환하여 Map으로 그룹화 (Bulk 조회 준비동작)

        // 유저 별 구독중인 상품 정보 수집
        Map<Long, List<UserSubscribeProductDto>> uspMap = uspRepository.findByUserIdIn(userIds, startDate, endDate)
                .stream()
                .map(UserSubscribeProductDto::fromEntity)
                .collect(Collectors.groupingBy(UserSubscribeProductDto::getUserId));

        // 유저 별 추가요금 정보 수집
        Map<Long, List<AdditionalChargeDto>> chargeMap = chargeRepository.findByUserIdIn(userIds, startDate, endDate)
                .stream()
                .map(AdditionalChargeDto::fromEntity)
                .collect(Collectors.groupingBy(AdditionalChargeDto::getUserId));

        // 유저 별 할인 정보 수집
        Map<Long, List<UserSubscribeDiscountDto>> discountMap = discountRepository.findByUserIdIn(userIds, startDate, endDate)
                .stream()
                .map(UserSubscribeDiscountDto::fromEntity)
                .collect(Collectors.groupingBy(UserSubscribeDiscountDto::getUserId));

        // 유저 사용량
        Map<Long, List<UserUsageDto>> UsageMap = userUsageRepository.findByUserIdIn(userIds, startDate.plusMonths(1), endDate.plusMonths(1))
                .stream()
                .map(UserUsageDto::fromEntity)
                .collect(Collectors.groupingBy(UserUsageDto::getUserId));

        Map<Long, BillingUserMemberDto> userMapById = users.stream()
                .collect(Collectors.toMap(
                        BillingUserMemberDto::getUserId,
                        user -> user
                ));


        // 3. DTO 조립 --> processor 로 넘길 정보
        for (BillingUserMemberDto user : users) {
            BillingUserBillingInfoDto dto = BillingUserBillingInfoDto.builder()
                    .userId(user.getUserId())
                    // Null 방지 --> List.of() 사용
                    .products(uspMap.getOrDefault(user.getUserId(), List.of()))
                    .additionalCharges(chargeMap.getOrDefault(user.getUserId(), List.of()))
                    .discounts(discountMap.getOrDefault(user.getUserId(), List.of()))
                    .usage(UsageMap.getOrDefault(user.getUserId(), List.of()))
                    .users(userMapById.get(user.getUserId()))
                    .build();

            buffer.add(dto);
            lastProcessedUserId = user.getUserId();
        }
        log.info("Buffer filled with {} records. LastProcessedUserId: {}", buffer.size(), lastProcessedUserId);
    }
}