package com.touplus.billing_message.sender;

import com.touplus.billing_message.common.crypto.Decrypto;
import com.touplus.billing_message.common.masking.MaskingUtils;
import com.touplus.billing_message.domain.entity.MessageSnapshot;
import com.touplus.billing_message.domain.entity.MessageType;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Mock 메시지 발송기
 * - 실제 외부 서버 대신 1초 딜레이로 발송 시뮬레이션
 * - ScheduledExecutorService를 사용하여 논블로킹 방식으로 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MockMessageSender implements MessageSender {

    private final Decrypto decrypto;
    private final ScheduledExecutorService emailDelayScheduler;

    /**
     * 동기 발송 (하위 호환용)
     * 내부적으로 sendAsync()를 호출하고 결과를 대기
     */
    @Override
    public SendResult send(MessageType type, MessageSnapshot snapshot) {
        try {
            return sendAsync(type, snapshot).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return SendResult.fail("INTERRUPTED", "send interrupted");
        } catch (Exception e) {
            log.error("발송 중 예외 발생", e);
            return SendResult.fail("ERROR", e.getMessage());
        }
    }

    /**
     * 비동기 발송 (권장)
     * - 복호화/마스킹은 즉시 수행
     * - 1초 딜레이는 ScheduledExecutorService로 논블로킹 처리
     * - 스레드를 점유하지 않고 1초 후 결과 반환
     */
    @Override
    public CompletableFuture<SendResult> sendAsync(MessageType type, MessageSnapshot snapshot) {
        // 복호화 (즉시 수행)
        String decryptedEmail = decrypto.decrypt(snapshot.getUserEmail());
        String decryptedPhone = decrypto.decrypt(snapshot.getUserPhone());

        // 마스킹 (로그/결과 메시지용)
        String maskedEmail = MaskingUtils.maskEmail(decryptedEmail);
        String maskedPhone = MaskingUtils.maskPhone(decryptedPhone);

        CompletableFuture<SendResult> future = new CompletableFuture<>();

        // 1초 후 실행 예약 (논블로킹!)
        emailDelayScheduler.schedule(() -> {
            try {
                if (type == MessageType.SMS) {
                    log.debug("SMS 발송 완료: phone={}", maskedPhone);
                    future.complete(SendResult.ok("OK", maskedPhone));
                    return;
                }

                // EMAIL: 1% 확률로 실패
                boolean failed = ThreadLocalRandom.current().nextInt(100) == 0;
                if (failed) {
                    log.warn("EMAIL 발송 실패: email={}", maskedEmail);
                    future.complete(SendResult.fail("MOCK_FAIL", maskedEmail));
                } else {
                    log.debug("EMAIL 발송 완료: email={}", maskedEmail);
                    future.complete(SendResult.ok("OK", maskedEmail));
                }
            } catch (Exception e) {
                log.error("발송 처리 중 예외", e);
                future.completeExceptionally(e);
            }
        }, 1, TimeUnit.SECONDS);

        return future;  // 즉시 반환 (스레드 점유 없음)
    }
}
