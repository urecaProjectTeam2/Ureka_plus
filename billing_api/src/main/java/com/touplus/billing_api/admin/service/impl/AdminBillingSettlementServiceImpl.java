package com.touplus.billing_api.admin.service.impl;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.touplus.billing_api.domain.repository.billing.UserSubscribeProductRepository;
import com.touplus.billing_api.domain.repository.message.UserRepository;
import org.springframework.stereotype.Service;

import com.touplus.billing_api.admin.dto.AdminUserSettlementResponse;
import com.touplus.billing_api.admin.dto.BillingProductStatResponse;
import com.touplus.billing_api.admin.dto.PageResponseDto;
import com.touplus.billing_api.admin.service.AdminBillingSettlementService;
import com.touplus.billing_api.domain.billing.dto.SettlementDetailsDto;
import com.touplus.billing_api.domain.billing.entity.BillingResult;
import com.touplus.billing_api.domain.billing.service.SettlementDetailsMapper;
import com.touplus.billing_api.domain.message.entity.User;
import com.touplus.billing_api.domain.message.service.UserContactService;
import com.touplus.billing_api.domain.repository.billing.BillingResultRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminBillingSettlementServiceImpl implements AdminBillingSettlementService {

    private final UserRepository userRepository;
    private final BillingResultRepository billingResultRepository;
    private final SettlementDetailsMapper settlementDetailsMapper;
    private final UserContactService userContactService;

    @Override
    public PageResponseDto<AdminUserSettlementResponse> getMonthlySettlementResults(
            LocalDate settlementMonth, int page, int size) {

        // 1. 이번 달 사용자 조회
        List<User> users = userRepository.findAllPaged(page * size, size);
        if (users == null) users = Collections.emptyList();

        // 2. 사용자 ID -> BillingResult 조회
        List<Long> userIds = users.stream().map(User::getUserId).toList();
        Map<Long, BillingResult> resultMap = billingResultRepository
                .findByUserIdsAndMonth(userIds, settlementMonth)
                .stream()
                .collect(Collectors.toMap(
                        BillingResult::getUserId,
                        r -> r,
                        (a, b) -> b   // 중복 시 마지막 값
                ));

        // 3. AdminUserSettlementResponse DTO 생성
        List<AdminUserSettlementResponse> contents = users.stream().map(user -> {
            BillingResult result = resultMap.get(user.getUserId());

            boolean billed = result != null && result.getSettlementDetails() != null;

            List<BillingProductStatResponse> products;
            if (billed) {
                SettlementDetailsDto detailsDto =
                        settlementDetailsMapper.fromJson(result.getSettlementDetails());
                products = detailsDto.toProductStatList();
            } else {
                products = Collections.emptyList();
            }

            return AdminUserSettlementResponse.builder()
                    .billingResultId(result != null ? result.getId() : null)
                    .settlementMonth(settlementMonth)
                    .user(userContactService.decryptAndMask(user))
                    .totalPrice(result != null ? result.getTotalPrice() : 0)
                    .billed(billed)
                    .details(products)
                    .build();
        }).toList();

        // 4. PageResponseDto 반환
        long totalUsers = userRepository.countAll();
        int totalPages = (int) Math.ceil((double) totalUsers / size);
        return PageResponseDto.<AdminUserSettlementResponse>builder()
                .contents(contents)
                .page(page)
                .size(size)
                .totalElements(totalUsers)
                .totalPages(totalPages)
                .build();
    }
}
