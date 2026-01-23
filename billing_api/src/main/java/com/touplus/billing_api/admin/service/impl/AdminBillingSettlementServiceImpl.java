package com.touplus.billing_api.admin.service.impl;

import com.touplus.billing_api.admin.dto.AdminUserSettlementResponse;
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

    public List<AdminUserSettlementResponse> getMonthlySettlementResults(
            LocalDate settlementMonth
    ) {
        // 1. 이번 달 정산 결과 조회
        List<BillingResult> results =
                billingResultRepository.findBySettlementMonth(settlementMonth);

        if (results.isEmpty()) {
            return List.of();
        }

        // 2. userId 추출
        List<Long> userIds = results.stream()
                .map(BillingResult::getUserId)
                .distinct()
                .toList();

        // 3. 사용자 정보 조회
        Map<Long, User> userMap =
                userRepository.findByIds(userIds).stream()
                        .collect(Collectors.toMap(
                                User::getUserId,
                                u -> u
                        ));

        // 4. DTO 조립
        return results.stream()
                .map(result -> {
                    User user = userMap.get(result.getUserId());

                    return AdminUserSettlementResponse.builder()
                            .billingResultId(result.getId())
                            .settlementMonth(result.getSettlementMonth())
                            .user(
                                    userContactService.decryptAndMask(user)
                            )
                            .totalPrice(result.getTotalPrice())
                            .details(
                                    settlementDetailsMapper.fromJson(
                                            result.getSettlementDetails()
                                    )
                            )
                            .build();
                })
                .toList();
    }
}
