//package com.touplus.billing_message.controller;
//
//import com.touplus.billing_message.domain.respository.MessageRepository;
//import com.touplus.billing_message.service.MessageDispatchService;
//import java.time.LocalDateTime;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.RestController;
//
///**
// * Test-only endpoints to trigger message dispatch locally.
// */
//@RestController
//@RequiredArgsConstructor
//@Slf4j
//public class TestDispatchController {
//
//    private final MessageDispatchService messageDispatchService;
//    private final MessageRepository messageRepository;
//
//    @GetMapping("/test/dispatch")
//    public String dispatchAllDue() {
//        log.info("Test dispatch trigger");
//        messageDispatchService.dispatchDueMessages();
//        return "Dispatch triggered";
//    }
//
//    @GetMapping("/test/dispatch/{messageId}")
//    public String dispatchOne(@PathVariable Long messageId) {
//        messageRepository.defer(messageId, LocalDateTime.now());
//        messageDispatchService.dispatchDueMessages();
//        return "Dispatch triggered for messageId=" + messageId;
//    }
//
//    /**
//     * 스케줄 무시하고 WAITED 상태의 모든 메시지 발송 (테스트용)
//     */
//    /* 일단 각주로 해놓음
//    @GetMapping("/test/dispatch-all")
//    public String dispatchAllWaited() {
//        log.info("스케쥴 무시하고 모든 wiated 상태 발송(테스트)");
//        int count = messageDispatchService.dispatchAllWaited();
//        return "Dispatch triggered for " + count + " messages";
//    }*/
//}
