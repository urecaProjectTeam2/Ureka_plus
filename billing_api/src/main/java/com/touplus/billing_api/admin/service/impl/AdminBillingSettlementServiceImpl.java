package com.touplus.billing_api.admin.service.impl;

import com.touplus.billing_api.admin.dto.AdminUserSettlementResponse;
import com.touplus.billing_api.admin.dto.PageResponse;
import com.touplus.billing_api.admin.service.AdminBillingSettlementService;
import com.touplus.billing_api.domain.billing.entity.BillingResult;
import com.touplus.billing_api.domain.billing.service.SettlementDetailsMapper;
import com.touplus.billing_api.domain.message.entity.User;
import com.touplus.billing_api.domain.message.service.UserContactService;
import com.touplus.billing_api.domain.repository.billing.BillingResultRepository;
import com.touplus.billing_api.domain.repository.message.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminBillingSettlementServiceImpl implements AdminBillingSettlementService {
    private final BillingResultRepository billingResultRepository;
    private final UserRepository userRepository;
    private final UserContactService userContactService;
    private final SettlementDetailsMapper settlementDetailsMapper;

    public PageResponse<AdminUserSettlementResponse> getMonthlySettlementResults(
            LocalDate settlementMonth,
            int page,
            int size
    ) {
        int offset = page * size;

        // 1. 전체 유저 페이징 조회
        List<User> users = userRepository.findAllPaged(offset, size);
        long totalUsers = userRepository.countAll();

        if (users.isEmpty()) {
            return PageResponse.<AdminUserSettlementResponse>builder()
                    .contents(List.of())
                    .page(page)
                    .size(size)
                    .totalElements(totalUsers)
                    .totalPages((int) Math.ceil((double) totalUsers / size))
                    .build();
        }

        // 2. userId 추출
        List<Long> userIds = users.stream()
                .map(User::getUserId)
                .toList();

        // 3. 이번 달 정산 결과 조회
        Map<Long, BillingResult> resultMap =
                billingResultRepository
                        .findByUserIdsAndMonth(userIds, settlementMonth)
                        .stream()
                        .collect(Collectors.toMap(
                                BillingResult::getUserId,
                                r -> r
                        ));

        // 4. DTO 조립 (정산 없으면 null)
        List<AdminUserSettlementResponse> contents =
                users.stream()
                        .map(user -> {
                            BillingResult result = resultMap.get(user.getUserId());

                            return AdminUserSettlementResponse.builder()
                                    .billingResultId(
                                            result != null ? result.getId() : null
                                    )
                                    .settlementMonth(settlementMonth)
                                    .user(
                                            userContactService.decryptAndMask(user)
                                    )
                                    .totalPrice(
                                            result != null ? result.getTotalPrice() : null
                                    )
                                    .details(
                                            result != null
                                                    ? settlementDetailsMapper.fromJson(
                                                    result.getSettlementDetails()
                                            )
                                                    : null
                                    )
                                    .build();
                        })
                        .toList();

        return PageResponse.<AdminUserSettlementResponse>builder()
                .contents(contents)
                .page(page)
                .size(size)
                .totalElements(totalUsers)
                .totalPages((int) Math.ceil((double) totalUsers / size))
                .build();
    }

}
