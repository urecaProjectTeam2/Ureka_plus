package com.touplus.billing_batch.jobs.message.step;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.stereotype.Component;

// 실시간 처리 진행률에 관한 로그를 생성

@Slf4j
@Component
public class MessageStepLogger implements ChunkListener {
    @Override
    public void afterChunk(ChunkContext context) {
        // 현재까지 쓰기(Write) 완료된 전체 건수를 가져옴
        long count = context.getStepContext().getStepExecution().getWriteCount();
        if (count % 1000 == 0) {
            log.info(">>>> [Progress] 현재까지 메시지 전송 완료 건수: {}", count);
        }
    }
}