1. Config (멀티스레드 고속 처리)
**ThreadPoolTaskExecutor**를 활용해 
--> 단일 스레드가 아닌 20개의 스레드가 동시에 작업을 분할 처리하여 CPU 활용도를 극대화

2. Writer (비동기 전송 및 동기 업데이트)
--> Kafka 전송 시 CompletableFuture 비동기 논블로킹(Non-blocking) 방식으로 네트워크 병목을 해결
--> DB 업데이트는 Batch Update 구조를 통해 정합성을 보장

3. SkipListener (실패 격리 저장)
--> 예외 발생 시 전체 공정을 중단하지 않고, 해당 데이터만 에러 로그 테이블로 즉시 분리
--> 데이터 유실 없는 결함 허용(Fault Tolerance) 구조

4. Logger (실시간 지표 모니터링)
--> ChunkContext 내부의 카운터를 참조하여 멀티스레드 환경에서도 누적 처리량을 실시간으로 추적, 처리 속도와 진행률을 가시화

5. application.yml (인프라 최적화)
--> HikariCP 커넥션 풀을 스레드 수에 맞춰 최적화하고, Kafka의 Batching & Compression 설정을 통해 대역폭 효율 높임