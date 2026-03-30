<div align="center">
  <h1>🚀 To U+</h1>
  <p><strong>대용량 정산 및 메시지 발송 관리 시스템</strong></p>
</div>

---

## 📌 프로젝트 소개

**To U+** 는 대규모 사용자 데이터를 기반으로 정산을 수행하고,  
그 결과를 바탕으로 메시지를 생성·발송하는 과정을 통합 관리할 수 있도록 설계한 **백엔드 중심 프로젝트**입니다.

이 프로젝트는 단순한 관리자 페이지 구현이 아니라,  
**정산 배치**, **이벤트 기반 메시지 처리**, **운영 모니터링**을 하나의 흐름으로 연결하여  
대용량 처리 환경에서도 안정적으로 동작하는 시스템을 설계하는 데 초점을 맞추었습니다.

관리자는 웹 대시보드를 통해 정산 현황, 배치 진행 상태, Kafka 전송 상태, 메시지 생성 및 발송 상태를 확인할 수 있으며,  
템플릿 관리, 사용자 정보 조회, Audit 로그 확인 등을 통해 전체 운영 흐름을 추적할 수 있습니다.

---

## 🛠 기술 스택

### Backend
![Java](https://img.shields.io/badge/Java%2017-007396?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot%203.x-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![Spring Batch](https://img.shields.io/badge/Spring_Batch-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white)

### Infrastructure & Messaging
![MySQL](https://img.shields.io/badge/MySQL%208.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)
![Apache Kafka](https://img.shields.io/badge/Apache_Kafka-231F20?style=for-the-badge&logo=apache-kafka&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)

### Tools / DevOps
![Git](https://img.shields.io/badge/Git-F05032?style=for-the-badge&logo=git&logoColor=white)
![AWS EC2](https://img.shields.io/badge/AWS_EC2-FF9900?style=for-the-badge&logo=amazon-ec2&logoColor=white)

---

## ✨ 주요 기능

### 1. 대용량 정산 배치 처리
- 대규모 사용자 데이터를 대상으로 정산 배치 실행
- 정산 대상 월 기준으로 배치 작업 수행 및 결과 생성
- 정산 결과 저장 후 다음 단계로 연계 가능한 처리 흐름 구성
- Chunk 기반 처리, 단계별 상태 관리 등 대용량 환경을 고려한 구조 반영

### 2. Kafka 기반 메시지 처리
- 정산 결과를 기반으로 Kafka 전송 단계 수행
- 메시지 생성과 발송 단계를 분리하여 비동기 처리 흐름 구성
- 메시지 처리 상태를 단계별로 추적 가능
- 중복 처리 상황을 고려한 안정적인 메시지 발송 구조 설계

### 3. 관리자 대시보드
- 정산 월, 전체 대상 데이터 건수, 전체 진행 현황 확인
- Batch Job 진행 상태 모니터링
- Kafka 전송 상태, 메시지 생성 상태, 메시지 발송 상태 확인
- 운영자가 전체 처리 흐름을 한눈에 파악할 수 있도록 구성

### 4. Audit 로그 관리
- 주요 작업 이력 및 운영 로그 조회
- 장애 상황 발생 시 원인 추적을 위한 기반 제공
- 운영 이력 확인을 통한 관리 편의성 지원

### 5. 사용자 및 템플릿 관리
- 정산 및 발송 대상 사용자 정보 조회
- 메시지 템플릿 생성, 수정, 삭제 기능 제공
- 템플릿 기반 메시지 관리로 운영 효율성 향상

---

## 📂 프로젝트 구조

```bash
ToUPlus/
├── billing_api/       # 관리자 요청 처리 및 조회 API
├── billing_batch/     # 대용량 정산 배치 서버
├── billing_message/   # Kafka 기반 메시지 생성 및 발송 서버
├── docker-compose.yml
└── README.md
```

---

## 📊 아키텍처 및 워크플로우

<details>
<summary><b>전체 플로우차트 보기</b></summary>

<br/>
<img width="100%" src="https://github.com/user-attachments/assets/abc79424-3a09-4c4f-9102-95bcdab73a24" />

</details>

<details>
<summary><b>Batch 서버 워크플로우 보기</b></summary>

<br/>
<img width="100%" src="https://github.com/user-attachments/assets/eccbc497-6822-42de-b4c9-c3a25de4d469" />

</details>

<details>
<summary><b>Message 서버 워크플로우 보기</b></summary>

<br/>
<img width="100%" src="https://github.com/user-attachments/assets/686d9aa5-a6bd-40d4-b605-06f3d57c944f" />

</details>

<details>
<summary><b>전체 서버 구조 보기</b></summary>

<br/>
<img width="100%" src="https://github.com/user-attachments/assets/d69788d7-6d25-4c46-8195-c52c32412d90" />

</details>

---

## 🗄️ ERD

<details>
<summary><b>ERD (Entity Relationship Diagram) 보기</b></summary>

<br/>
<!-- TODO: ERD 이미지 주소를 아래 src에 넣어주세요 -->
<img width="100%" src="https://github.com/user-attachments/assets/57e06716-58f8-42f7-9b75-85fdf0891bf0" alt="ERD Diagram" />
<img width="100%" src="https://github.com/user-attachments/assets/a8b64e00-5fa4-4425-b3c1-9c55d1ba2e93" alt="ERD Diagram" />

</details>

---

## 🔄 핵심 처리 흐름

1. 정산 대상 월과 전체 대상 데이터 확인
2. Batch Job 실행 및 정산 진행
3. 정산 결과 기반 Kafka 전송
4. 메시지 생성 단계 처리
5. 메시지 발송 단계 처리
6. 운영자는 대시보드와 로그를 통해 전체 이력 확인
7. 필요 시 템플릿 수정

---

## 🧩 설계 포인트

### 대용량 배치 처리 중심 설계
- 대량 데이터를 한 번에 처리해야 하는 정산 시나리오를 전제로 구성
- 배치 실행 상태와 처리 진행률을 운영 관점에서 확인할 수 있도록 설계
- 단순 조회 시스템이 아니라 실제 대용량 처리 흐름을 관리하는 데 목적을 둠

### 비동기 메시지 발송 흐름 분리
- 정산과 메시지 발송을 하나의 동기 처리로 묶지 않고 단계적으로 분리
- Kafka를 통해 정산 이후 메시지 처리 단계를 유연하게 연결
- 메시지 생성과 발송 단계를 나누어 운영 상태 추적이 가능하도록 구성

### 운영 가시성 확보
- 정산, Kafka 전송, 메시지 생성, 메시지 발송까지의 흐름을 시각적으로 확인 가능
- Audit 로그와 상태 대시보드를 통해 운영 이력과 현재 상태를 함께 관리
- 장애 분석과 운영 추적을 고려한 구조 반영

### 템플릿 기반 운영 효율화
- 반복적으로 사용하는 메시지를 템플릿 형태로 관리
- 메시지 일관성을 유지하고 운영자의 수작업 부담을 줄일 수 있도록 구성

---

## 🎯 기대 효과

- **운영 효율성 향상**  
  정산부터 메시지 발송까지의 흐름을 하나의 관리자 시스템에서 통합 관리할 수 있습니다.

- **대용량 처리 가시성 확보**  
  각 단계의 진행 상황을 확인할 수 있어 운영자가 현재 처리 상태를 빠르게 파악할 수 있습니다.

- **운영 안정성 강화**  
  상태 모니터링과 Audit 로그를 통해 장애 대응과 운영 추적이 쉬워집니다.

- **반복 업무 자동화 기반 마련**  
  템플릿 관리와 메시지 처리 흐름을 통해 반복적인 운영 업무를 줄일 수 있습니다.

---

## 📈 향후 개선 방향

- 발송 채널 확장 (SMS / EMAIL / PUSH 등)
- 사용자별 발송 정책 설정 기능 추가
- 통계 및 리포트 기능 고도화
- 장애 알림 및 운영 모니터링 기능 강화

---

## 🎥 Demo

GIF를 통해 프로젝트의 실제 동작 과정 및 화면 구성을 확인할 수 있습니다.

<table>
  <tr>
    <td align="center">
      <img width="400" src="https://github.com/user-attachments/assets/e32ce8ea-1b49-4c5c-99ec-759cbf9cfee7" alt="메인 대시보드" />
    </td>
    <td align="center">
      <img width="400" src="https://github.com/user-attachments/assets/70450ae5-a2d8-46cf-8cf4-ab35250fd564" alt="배치 대시보드" />
    </td>
  </tr>
  <tr>
    <td align="center">
      <b>메인 대시보드</b><br>
      전체 진행사항을 요약해서 전달합니다. 
    </td>
    <td align="center">
      <b>배치 대시보드</b><br>
      정산 배치의 진행 상태 및 결과를 모니터링합니다.
    </td>
  </tr>

  <tr>
    <td align="center">
      <img width="400" src="https://github.com/user-attachments/assets/3b33ce2d-facf-48c0-928d-86b061fefc8a" alt="메세지 대시보드" />
    </td>
    <td align="center">
      <img width="400" src="https://github.com/user-attachments/assets/a770ee01-2e64-4d1a-b650-296c6bdefe3d" alt="관리자 로그 관리" />
    </td>
  </tr>
  <tr>
    <td align="center">
      <b>메세지 대시보드</b><br>
      Kafka 기반 배치 서버에서 전송된 내역을 수신 및 발송 상태를 실시간으로 확인합니다.
    </td>
    <td align="center">
      <b>관리자 로그 관리</b><br>
      시스템 내 주요 작업 이력 및 Audit 로그를 조회합니다.
      에러 시 에러를 확인할 수 있으며 그에 따른 로그를 자세히 보거나 txt 파일로 다운로드 받을 수 있습니다. 
    </td>
  </tr>

  <tr>
    <td align="center">
      <img width="400" src="https://github.com/user-attachments/assets/df9e7d35-8e77-44ea-8762-d9b08f585a8d" alt="메세지 템플릿 관리" />
    </td>
    <td align="center">
      <img width="400" src="https://github.com/user-attachments/assets/19fea02a-7ffd-4a90-be27-f72269a698c0" alt="사용자 대시보드" />
    </td>
  </tr>
  <tr>
    <td align="center">
      <b>메세지 템플릿 관리</b><br>
      발송할 메시지의 템플릿을 생성, 수정, 삭제 및 관리합니다.
    </td>
    <td align="center">
      <b>사용자 대시보드</b><br>
      사용자별 정산 및 메시지 발송 현황을 조회합니다.
      외에도 사용자가 자주 쓰는 요금제 등 사용자 데이터를 그래프로 확인할 수 있습니다. 
    </td>
  </tr>
</table>

<br />

---

<div align="center">
  <small>Developed & Maintained by To U+ Team</small>
</div>
