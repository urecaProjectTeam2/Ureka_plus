## API 에서 가져와 쓸 Data List
### Billing_batch
- **미납자 관련 정보**
  - unpaid
  - 
- **Batch 실행 이력**
  - user 별 정산 결과 (이용중인상품/가격?)
  - Biling_result -> details

- 논의(개발자인 운영자) 
  - 몇퍼 성공 / 실패 -> 실패한 사람 id나 name, email, phone Moking
  - 현재 배치 진행 사항 (실시간) < 논의 

> 추가 가능한 영역
>> - **user가 가장 많이 가입한 상품** 
>>  - user_subscribe_product
>>  - billing_product

### billing_message
- **메세지 템플릿 정보** 
  - message_template(CRUD 다 되게)
- **메세지 관련 실행 이력** 
  - message_send_log
- **유저 정보(Mocking 필수)**
  - users

### 사업자 페이지
- 미정산 사람 
- user 별 정산 결과 (이용중인상품/가격?)
- Biling_result -> details

- message_template(CRUD 다 되게)
- user가 가장 많이 가입한 상품
  - user_subscribe_product
  - billing_product

- 메세지 전송 완료 여부 : message status