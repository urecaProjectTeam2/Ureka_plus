package com.touplus.billing_api.admin.service.impl;

import com.touplus.billing_api.admin.dto.PageResponse;
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
    public PageResponse<UnpaidUserResponse> getUnpaidUsers(int page, int size) {

        int offset = page * size;

        // 1. 목록 조회
        List<Unpaid> unpaids = unpaidRepository.findUnpaidUsers(offset, size);

        // 2. 전체 개수 조회
        long totalElements = unpaidRepository.countUnpaidUsers();
        int totalPages = (int) Math.ceil((double) totalElements / size);

        if (unpaids.isEmpty()) {
            return PageResponse.<UnpaidUserResponse>builder()
                    .contents(List.of())
                    .page(page)
                    .size(size)
                    .totalElements(totalElements)
                    .totalPages(totalPages)
                    .build();
        }

        // 3. 사용자 정보 조회
        List<Long> userIds = unpaids.stream()
                .map(Unpaid::getUserId)
                .distinct()
                .toList();

        Map<Long, User> userMap = userRepository.findByIds(userIds).stream()
                .collect(Collectors.toMap(User::getUserId, Function.identity()));

        // 4. DTO 변환
        List<UnpaidUserResponse> contents = unpaids.stream()
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

        // 5. PageResponse 생성
        return PageResponse.<UnpaidUserResponse>builder()
                .contents(contents)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .build();
    }

//    @Override
//    public List<UnpaidUserResponse> getUnpaidUsersByMonth(int page, int size, String month) {
//        // 1. 특정 월 미납 조회
//        List<Unpaid> unpaids = unpaidRepository.findUnpaidUsersByMonth(page, size, month);
//        if (unpaids.isEmpty()) {
//            return List.of();
//        }
//
//        // 2. 사용자 ID만 추출
//        List<Long> userIds = unpaids.stream()
//                .map(Unpaid::getUserId)
//                .distinct()
//                .toList();
//
//        // 3. 사용자 정보 조회
//        Map<Long, User> userMap = userRepository.findByIds(userIds).stream()
//                .collect(Collectors.toMap(User::getUserId, Function.identity()));
//
//        // 4. DTO로 변환
//        return unpaids.stream()
//                .map(unpaid -> {
//                    User user = userMap.get(unpaid.getUserId());
//                    return new UnpaidUserResponse(
//                            unpaid.getId(),
//                            unpaid.getUnpaidPrice(),
//                            unpaid.getUnpaidMonth(),
//                            userContactService.decryptAndMask(user)
//                    );
//                })
//                .toList();
//    }

    @Override
    public PageResponse<UnpaidUserResponse> searchUnpaidUsersByKeyword(
            int page,
            int size,
            String keyword
    ) {
        int offset = page * size;

        List<Unpaid> unpaids =
                unpaidRepository.searchUnpaidUsersByKeyword(offset, size, keyword);

        long totalElements =
                unpaidRepository.countUnpaidUsersByKeyword(keyword);

        int totalPages = (int) Math.ceil((double) totalElements / size);

        if (unpaids.isEmpty()) {
            return PageResponse.<UnpaidUserResponse>builder()
                    .contents(List.of())
                    .page(page)
                    .size(size)
                    .totalElements(totalElements)
                    .totalPages(totalPages)
                    .build();
        }

        List<Long> userIds = unpaids.stream()
                .map(Unpaid::getUserId)
                .distinct()
                .toList();

        Map<Long, User> userMap = userRepository.findByIds(userIds).stream()
                .collect(Collectors.toMap(User::getUserId, Function.identity()));

        List<UnpaidUserResponse> contents = unpaids.stream()
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

        return PageResponse.<UnpaidUserResponse>builder()
                .contents(contents)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .build();
    }

}