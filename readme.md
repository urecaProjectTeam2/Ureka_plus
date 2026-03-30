<div align="center">
  <h1>🚀 Ureka_plus</h1>
  <p><strong>대용량 통신 요금 명세서 및 알림 발송 시스템</strong></p>
</div>

---

### 📌 서비스 개요
이 프로젝트는 대규모 요금 정산 데이터 배치를 처리하고, 정산 결과를 바탕으로 고객에게 명세서와 알림을 안정적으로 발송하는 시스템입니다. 모노레포 아키텍처를 기반으로 설계되었으며, Kafka와 같은 메시지 브로커를 활용해 모듈 간 결합도를 낮추고 처리율을 높였습니다.

**질문사항** <br>
👉 [Notion 링크](https://auspicious-blinker-37f.notion.site/2e921d7d5c4480ee9dabc1cdac0e514b?source=copy_link)

---

### 🛠 기술 스택 (Tech Stack)

#### 🔹 Backend
![Java](https://img.shields.io/badge/Java%2017-007396?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot%203.x-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![Spring Batch](https://img.shields.io/badge/Spring_Batch-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white)

#### 🔹 Infrastructure & Messaging
![MySQL](https://img.shields.io/badge/MySQL%208.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)
![Apache Kafka](https://img.shields.io/badge/Apache_Kafka-231F20?style=for-the-badge&logo=apache-kafka&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)

#### 🔹 Tools / DevOps
![Git](https://img.shields.io/badge/Git-F05032?style=for-the-badge&logo=git&logoColor=white)
![AWS EC2](https://img.shields.io/badge/AWS_EC2-FF9900?style=for-the-badge&logo=amazon-ec2&logoColor=white)

---

### 💡 주요 핵심 기능 (Core Features)

#### 📝 1. 정산 (Billing Batch)
- **정산 데이터 생성의 단일 진실 공급원 (SSOT)**: 모든 요금 계산과 정산 결과는 Batch Server에서 최초로 생성됩니다.
- **데이터 정합성 보장**: 데이터베이스에 정상적으로 커밋된 이후에만 Kafka로 “정산 완료” 이벤트가 발행되도록 하여 트랜잭션 무결성을 확보했습니다.
- **불변성(Immutability)**: Kafka를 통해 전달되는 모든 데이터는 이미 확정된 값으로, 이후 단계에서 데이터가 위변조되지 않습니다.

#### ✉️ 2. 메시지 시스템 (Message Broker)
- **멀티 채널 발송**: 수신된 이벤트를 바탕으로 실제 이메일, 문자, 푸시 메시지 등 다양한 채널로 발송합니다.
- **강력한 멱등성 보장**: 메시지 DB를 이용하여 중복 발송을 완벽히 차단합니다. 동일한 정산 이벤트가 재인입되더라도 단 한 번만(Exactly-once) 처리됩니다.
- **이력 관리 및 재시도 (Resilience)**: 메시지 발송 결과는 모두 이력으로 보관되며, 실패한 건에 대해서는 운영자가 수동으로 대상 건들을 재시도할 수 있습니다.

#### ⚙️ 3. 관리자 플랫폼 (Admin Server)
- **운영 전용 제어 플랫폼**: 전체 서비스의 동작 상태를 모니터링하고 제어하는 별도의 내부망 인트라넷 서버입니다.
- **배치 및 메시지 제어**: 배치 스케줄 실행 상태와 메시지 발송 상태를 조회하고, 시작/중지/재시도 등의 명령을 각 서버에 직접 전달할 수 있습니다.
- **동시성 제어 방어**: 관리자의 모든 제어 요청은 명령 ID를 기반으로 단일 처리되어, 새로고침이나 중복 클릭으로 인한 다중 요청에도 안전하게 구동됩니다.

---

### 📂 프로젝트 구조 (Project Structure)

본 프로젝트는 **모노레포(Monorepo)** 구조로 관리되며, 다음과 같은 서브 모듈들로 구성됩니다.

```text
Ureka_plus/
├── billing_api/       # [Module] 사용자 요청 처리 및 데이터 조회 API 서버
├── billing_batch/     # [Module] 대용량 정산 데이터 배치 처리 서버 (Spring Batch)
├── billing_message/   # [Module] Kafka 컨슈머 프로세스 및 실제 알림 발송 워커 서버
├── billing_common/    # [Module] 공통 도메인 모델, 유틸리티, 공유 설정 (예정)
├── docker-compose.yml # 로컬/개발 환경 통합 인프라 스피닝 설정 파일
└── README.md
```

---

### 📊 아키텍처 및 워크플로우 (Architecture & Workflow)

<details>
<summary><b>전체 플로우차트 보기</b> (클릭하여 펼치기)</summary>

<br/>
<img width="100%" alt="전체 플로우차트" src="https://github.com/user-attachments/assets/a427d5cb-029c-4b71-9a3b-632f30ff6024" />

</details>

<details>
<summary><b>Batch 서버 워크플로우 보기</b> (클릭하여 펼치기)</summary>

<br/>
<img width="100%" alt="Batch 서버 워크플로우" src="https://github.com/user-attachments/assets/f6b02faa-1551-4fdc-ae33-240978de9d9a" />

</details>

<details>
<summary><b>Message 서버 워크플로우 보기</b> (클릭하여 펼치기)</summary>

<br/>
<img width="100%" alt="Message 서버 워크플로우" src="https://github.com/user-attachments/assets/0694ba33-048b-4bb3-8b4c-89372b018b3f" />

</details>

<details>
<summary><b>전체 서버 구조 보기</b> (클릭하여 펼치기)</summary>

<br/>
<img width="100%" alt="전체 서버 구조" src="https://github.com/user-attachments/assets/cb0d74d3-736c-44a4-af24-38f28530553e" />

</details>

---

<div align="center">
  <small>Developed & Maintained by Ureka_plus Team</small>
</div>