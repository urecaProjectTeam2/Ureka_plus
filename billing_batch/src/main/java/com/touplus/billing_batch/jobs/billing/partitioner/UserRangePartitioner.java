package com.touplus.billing_batch.jobs.billing.partitioner;

import com.touplus.billing_batch.domain.dto.MinMaxIdDto;
import com.touplus.billing_batch.domain.repository.BillingUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j // 로그 추가
public class UserRangePartitioner implements Partitioner {

    private final BillingUserRepository billingUserRepository;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        // 1. DB에서 전체 ID 범위 조회 (500만 건 기준)
        MinMaxIdDto minMax = billingUserRepository.findMinMaxId();

        // #추가
        // 실제 데이터 분포를 고려해 파티셔닝 분포를 조정 --> 실패 시 로그 남기도록
        if (minMax == null || minMax.getMinId() == null || minMax.getMaxId() == null) {
            log.warn(">>> [Partitioner] 처리할 데이터가 없어 파티션을 생성하지 않습니다.");
            return Map.of();
        }

        long min = minMax.getMinId();
        long max = minMax.getMaxId();

        // 2. 한 파티션당 처리할 데이터 크기 계산
        long totalRange = max - min + 1;


        // 실제 처리할 데이터 개수가 gridSize보다 적을 경우 최적화
        int actualGridSize = (int) Math.min(gridSize, totalRange);
        long targetSize = (totalRange + actualGridSize - 1) / actualGridSize;

        log.info(">>> [Partitioner] 전체 범위: {} ~ {}, 총 구간 길이: {}, 파티션당 할당 크기: {}",
                min, max, totalRange, targetSize);

        Map<String, ExecutionContext> result = new HashMap<>();
        long start = min;
        long end = start + targetSize - 1;

        // 3. gridSize만큼 범위를 나누어 ExecutionContext 생성
        for (int i = 0; i < actualGridSize; i++) {
            ExecutionContext context = new ExecutionContext();

            // 마지막 파티션은 max값에 딱 맞게 조정
            long currentEnd = Math.min(end, max);

            context.putLong("minValue", start);
            context.putLong("maxValue", currentEnd);

            log.debug(">>> [Partitioner] 파티션 {} 할당: {} ~ {}", i, start, currentEnd);

            result.put("partition" + i, context);

            start += targetSize;
            end += targetSize;
        }

        return result;
    }
}
