package com.touplus.billing_batch.jobs.message.step;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

@Component
@StepScope // 실행 시점에 JobParameter를 주입받기 위해 필수
@RequiredArgsConstructor
public class TopicCreateTasklet implements Tasklet {

    private final KafkaAdmin kafkaAdmin;
    private static final String BASE_TOPIC = "billing-result-topic-";

    @Value("#{jobParameters['settlementMonth']}") // 실행 시점에 날짜 주입
    private String settlementMonth;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        // [수정] 복잡한 substring 로직을 제거하고 settlementMonth를 그대로 사용합니다.
        // 예: settlementMonth가 "2025-12-01"이면 토픽명은 "billing-result-topic-2025-12-01"이 됩니다.
        String TOPIC = BASE_TOPIC + settlementMonth;
//        String TOPIC = BASE_TOPIC + settlementMonth +"T2";

        NewTopic newTopic = TopicBuilder.name(TOPIC)
                .partitions(10) // 3 --> 10개로 늘려 테스트
                .replicas(1)
                .config(
                        TopicConfig.RETENTION_MS_CONFIG,
                        String.valueOf(3 * 24 * 60 * 60 * 1000L) // 3일간 보관
                )
                .build();

        // 토픽이 없으면 생성, 있으면 설정을 유지/수정합니다.
        kafkaAdmin.createOrModifyTopics(newTopic);

        return RepeatStatus.FINISHED;
    }
}