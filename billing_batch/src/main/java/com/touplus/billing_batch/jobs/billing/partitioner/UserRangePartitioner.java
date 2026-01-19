package com.touplus.billing_batch.jobs.billing.partitioner;

import com.touplus.billing_batch.domain.dto.MinMaxIdDto;
import com.touplus.billing_batch.domain.repository.BillingUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class UserRangePartitioner implements Partitioner {

    private final BillingUserRepository billingUserRepository;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        // 1. DB에서 전체 ID 범위 조회 (500만 건 기준)
        MinMaxIdDto minMax = billingUserRepository.findMinMaxId();
        long min = minMax.getMinId();
        long max = minMax.getMaxId();

        // 2. 한 파티션당 처리할 데이터 크기 계산
        long targetSize = (max - min) / gridSize + 1;

        Map<String, ExecutionContext> result = new HashMap<>();
        long start = min;
        long end = start + targetSize - 1;

        // 3. gridSize만큼 범위를 나누어 ExecutionContext 생성
        for (int i = 0; i < gridSize; i++) {
            ExecutionContext context = new ExecutionContext();
            context.putLong("minValue", start);
            context.putLong("maxValue", Math.min(end, max));

            result.put("partition" + i, context);

            start += targetSize;
            end += targetSize;
        }

        return result;
    }
}
