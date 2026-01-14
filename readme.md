# 주제
대용량 통신 요금 명세서 및 알림 발송 시스템

# 기능

## 1. 정산

- 정산 데이터의 유일한 기준
- 모든 요금 계산과 정산 결과는 Batch Server에서 생성, 데이터베이스에 정상적으로 커밋된 이후에만 Kafka로 “정산 완료” 이벤트가 발행
- Kafka를 통해 전달되는 모든 데이터는 이미 확정된 값

## 2. 메시지
- 이벤트를 받아 실제 이메일이나 문자, 푸시 메시지를 발송
- 메시지 DB를 이용해 중복 발송을 방지, 동일한 정산 이벤트가 여러 번 들어와도 한 번만 처리
- 메시지 발송 결과는 모두 기록되며, 실패한 건은 운영자가 재시도 가능

## 3. 관리자
- 운영 전용 서버
- 배치 실행 상태와 메시지 발송 상태를 조회, 시작·중지·재시도 같은 제어 명령을 각 서버에 전달
- 모든 제어는 명령 ID 기반으로 처리되어 중복 요청이 들어와도 실제 실행은 한 번만 이루어짐

---
## 기술 스택 (Tech Stack)

### Backend
- **Language**: Java 17
- **Framework**: Spring Boot 3.x
- **Batch Processing**: Spring Batch
- **Build Tool**: Gradle (Multi-module)

### Infrastructure & Messaging
- **Database**:
    - **Main**: MySQL 8.0 (정산 및 유저 데이터)
    - **Cache**: Redis (분산 락, 임시 저장)
- **Message Broker**: Apache Kafka, Zookeeper
- **Containerization**: Docker, Docker Compose

### Tools
- **Version Control**: Git
- **Deployment**: Cafe24(DB), AWS EC2(App)

---

## 프로젝트 구조 (Project Structure)

본 프로젝트는 **모노레포(Monorepo)** 구조로 관리되며, 다음과 같은 모듈로 구성

```
Ureka_plus/
├── billing_api/      # [Module] 사용자 요청 처리 및 데이터 조회 API
├── billing_batch/    # [Module] 대용량 정산 데이터 배치 처리 (Spring Batch)
├── billing_message/  # [Module] Kafka 메시지 소비 및 알림 발송
├── billing_common/   # [Module] 공통 도메인, 유틸리티, 설정 (예정)
├── docker-compose.yml # 로컬/개발 환경 인프라 구성
└── README.md
```

---

## 플로우차트
<img width="15245" height="9216" alt="image" src="https://github.com/user-attachments/assets/a427d5cb-029c-4b71-9a3b-632f30ff6024" />

- batch 서버
<img width="2635" height="2600" alt="워크플로우-half" src="https://github.com/user-attachments/assets/f6b02faa-1551-4fdc-ae33-240978de9d9a" />

- message 서버
<img width="2971" height="1936" alt="워크플로우-2" src="https://github.com/user-attachments/assets/0694ba33-048b-4bb3-8b4c-89372b018b3f" />

## 서버구조
<img width="998" height="615" alt="image" src="https://github.com/user-attachments/assets/8dcd30e9-94ee-4da7-ba6c-b47368421aef" />

## ERD
<img width="798" height="581" alt="image" src="https://github.com/user-attachments/assets/ae0498ae-8970-4e2d-a3ff-aa32b5d0cab8" />

<img width="950" height="912" alt="image" src="https://github.com/user-attachments/assets/fc162067-e6eb-4499-8ff7-f5f75283cf7f" />


