package com.touplus.billing_message.controller;

import com.touplus.billing_message.domain.entity.BillingSnapshot;
import com.touplus.billing_message.domain.respository.BillingSnapshotRepository;
import com.touplus.billing_message.processor.MessageProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * 테스트용 컨트롤러
 * Kafka 없이 MessageProcessor 테스트
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class TestController {

    private final BillingSnapshotRepository snapshotRepository;
    private final MessageProcessor messageProcessor;

    /**
     * billing_snapshot에서 데이터 가져와서 MessageProcessor 테스트
     * 사용: http://localhost:8080/test/process/{billingId}
     */
//    @GetMapping("/test/process/{billingId}")
//    public String testProcess(@PathVariable Long billingId) {
//        log.info("테스트 시작 - billingId={}", billingId);
//
//        BillingSnapshot snapshot = snapshotRepository.findById(billingId)
//            .orElseThrow(() -> new RuntimeException("Snapshot 없음: " + billingId));
//
//        messageProcessor.process(snapshot);
//
//        return "Message 생성 완료! billingId=" + billingId;
//    }
}
