package com.touplus.billing_api.admin.service.impl;

import com.touplus.billing_api.admin.dto.UnpaidUserResponse;
import com.touplus.billing_api.admin.service.AdminUnpaidService;
import com.touplus.billing_api.domain.billing.entity.Unpaid;
import com.touplus.billing_api.domain.message.entity.User;
import com.touplus.billing_api.domain.message.service.UserContactService;
import com.touplus.billing_api.domain.repository.billing.UnpaidRepository;
import com.touplus.billing_api.domain.repository.message.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUnpaidServiceImpl implements AdminUnpaidService {

    private final UnpaidRepository unpaidRepository;
    private final UserRepository userRepository;
    private final UserContactService userContactService;

    @Override
    public List<UnpaidUserResponse> getUnpaidUsers(int page, int size) {

        List<Unpaid> unpaids = unpaidRepository.findUnpaidUsers(page, size);
        if (unpaids.isEmpty()) {
            return List.of();
        }

        List<Long> userIds = unpaids.stream()
                .map(Unpaid::getUserId)
                .distinct()
                .toList();

        Map<Long, User> userMap = userRepository.findByIds(userIds).stream()
                .collect(Collectors.toMap(User::getUserId, Function.identity()));

        return unpaids.stream()
                .map(unpaid -> {
                    User user = userMap.get(unpaid.getUserId());

                    return new UnpaidUserResponse(
                            unpaid.getId(),
                            unpaid.getUnpaidPrice(),
                            unpaid.getUnpaidMonth(),
                            userContactService.decryptAndMask(user)
                    );
                })
                .toList();
    }

    @Override
    public List<UnpaidUserResponse> getUnpaidUsersByMonth(int page, int size, String month) {
        // 1. 특정 월 미납 조회
        List<Unpaid> unpaids = unpaidRepository.findUnpaidUsersByMonth(page, size, month);
        if (unpaids.isEmpty()) {
            return List.of();
        }

        // 2. 사용자 ID만 추출
        List<Long> userIds = unpaids.stream()
                .map(Unpaid::getUserId)
                .distinct()
                .toList();

        // 3. 사용자 정보 조회
        Map<Long, User> userMap = userRepository.findByIds(userIds).stream()
                .collect(Collectors.toMap(User::getUserId, Function.identity()));

        // 4. DTO로 변환
        return unpaids.stream()
                .map(unpaid -> {
                    User user = userMap.get(unpaid.getUserId());
                    return new UnpaidUserResponse(
                            unpaid.getId(),
                            unpaid.getUnpaidPrice(),
                            unpaid.getUnpaidMonth(),
                            userContactService.decryptAndMask(user)
                    );
                })
                .toList();
    }

    @Override
    public List<UnpaidUserResponse> searchUnpaidUsersByKeyword(int page, int size, String keyword) {
        // 1. DB에서 검색
        List<Unpaid> unpaids = unpaidRepository.searchUnpaidUsersByKeyword(page, size, keyword);
        if (unpaids.isEmpty()) return List.of();

        // 2. 사용자 ID 추출
        List<Long> userIds = unpaids.stream()
                .map(Unpaid::getUserId)
                .distinct()
                .toList();

        // 3. 사용자 정보 조회
        Map<Long, User> userMap = userRepository.findByIds(userIds).stream()
                .collect(Collectors.toMap(User::getUserId, Function.identity()));

        // 4. DTO 변환
        return unpaids.stream()
                .map(unpaid -> {
                    User user = userMap.get(unpaid.getUserId());
                    return new UnpaidUserResponse(
                            unpaid.getId(),
                            unpaid.getUnpaidPrice(),
                            unpaid.getUnpaidMonth(),
                            userContactService.decryptAndMask(user)
                    );
                })
                .toList();
    }

}