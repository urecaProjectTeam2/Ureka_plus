package com.touplus.billing_batch.jobs.billing.step.reader;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import com.touplus.billing_batch.common.BillingException;
import com.touplus.billing_batch.common.BillingFatalException;
import com.touplus.billing_batch.domain.dto.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Component
@StepScope
public class BillingItemReader implements ItemStreamReader<BillingUserBillingInfoDto> {

    private final BillingUserRepository userRepository;
    private final UserSubscribeProductRepository uspRepository;
    private final UnpaidRepository unpaidRepository;
    private final AdditionalChargeRepository chargeRepository;
    private final UserSubscribeDiscountRepository discountRepository;

    private final Deque<BillingUserBillingInfoDto> buffer = new ArrayDeque<>();
    private final int chunkSize = 2000;

    @Value("#{stepExecutionContext['minValue']}")
    private Long minValue;

    @Value("#{stepExecutionContext['maxValue']}")
    private Long maxValue;

    @Value("#{jobParameters['targetMonth']}")
    private String targetMonth;

    @Value("#{jobParameters['forceFullScan'] ?: false}")
    private boolean forceFullScan;

    // DB 조회용
    private Long lastProcessedUserId = 0L;  // 실제 read()로 반환된 ID
    private StepExecution stepExecution;
    private static final String CTX_LAST_PROCESSED_USER_ID = "lastProcessedUserId";

    private LocalDate startDate;
    private LocalDate endDate;

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
        if (forceFullScan) {
            log.info(">> [NOTICE] 전체 재정산 모드(forceFullScan=true)로 동작합니다. 모든 데이터를 다시 처리합니다.");
        }

        try {
            // targetMonth 시작일-종료일 계산
            this.startDate = LocalDate.parse(targetMonth);
            this.endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
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
    public void close() throws ItemStreamException {
        // 특별히 할 일 없음
    }

    @Override
    public BillingUserBillingInfoDto read() {
        try {
            if (buffer.isEmpty()) {
                fillBuffer();
            }

            BillingUserBillingInfoDto dto = buffer.poll();
            if (dto != null) {
                lastProcessedUserId = dto.getUserId();
            }
            return dto;
        }catch (BillingFatalException e){
            throw e;
        }catch (Exception e){
            throw new BillingFatalException("Reader 실행 중 예상치 못한 오류 발생", "ERR_READER_UNKNOWN", 0L);
        }
    }

    private void fillBuffer() throws Exception{

        // 1. No-Offset 방식: userId 기준 다음 청크 조회
        List<BillingUser> users =
                userRepository.findUsersInRange(
                        minValue,
                        maxValue,
                        lastProcessedUserId,
                        forceFullScan,
                        startDate,
                        endDate,
                        Pageable.ofSize(chunkSize)
                );

        if (users.isEmpty()) {
            // 처음부터 값이 없는 경우
            if(lastProcessedUserId == minValue - 1){
                throw BillingFatalException.dataNotFound("정산 대상 유저가 존재하지 않습니다. 설정을 확인하세요.");
            }
            return;
        }

        List<Long> userIds = users.stream().map(BillingUser::getUserId).toList();

        // 엔티티 -> DTO 변환 + Map 생성
        Map<Long, List<UserSubscribeProductDto>> uspMap = uspRepository.findByUserIdIn(userIds, startDate, endDate).stream()
                .map(UserSubscribeProductDto::fromEntity)
                .collect(Collectors.groupingBy(UserSubscribeProductDto::getUserId));

        if(uspMap.isEmpty()){
            throw BillingFatalException.dataNotFound(String.format("데이터 정합성 오류: 대상 유저 %d명에 대한 구독 상품 정보가 존재하지 않습니다.", users.size()));
        }

        Map<Long, List<UnpaidDto>> unpaidMap = unpaidRepository.findByUserIdIn(userIds).stream()
                .map(UnpaidDto::fromEntity)
                .collect(Collectors.groupingBy(UnpaidDto::getUserId));

        Map<Long, List<AdditionalChargeDto>> chargeMap = chargeRepository.findByUserIdIn(userIds, startDate, endDate).stream()
                .map(AdditionalChargeDto::fromEntity)
                .collect(Collectors.groupingBy(AdditionalChargeDto::getUserId));

        Map<Long, List<UserSubscribeDiscountDto>> discountMap = discountRepository.findByUserIdIn(userIds, startDate, endDate).stream()
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