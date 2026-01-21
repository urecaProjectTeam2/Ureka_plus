## API 에서 가져와 쓸 Data List
### Billing_batch
- **미납자 관련 정보**
  - unpaid
  - 
- **Batch 실행 이력**
  - BATCH_JOB_EXECUTION
  - BATCH_STEP_EXECUTION
  - batch_billing_error_log < 필요하면?

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