package com.touplus.billing_api.admin.dto;

public class WholeProcessContentDto {
/*batch job 진행
카프카는 상태를 어떻게 나타내지...?
kafkasent
kafkaReceive
createdMessage
sentMessage*/

	private long billingResultId;
	private String billingStatus; // billingResultId를 기준으로 sent의 비율 나타냄 
	
	private long createMessageId; // message의 message_id와 billingResultId 개수랑 같은지 비율 계산
	
	private long messageStatus; // message의 message_id와 sent 개수로 비율 나타냄
	
	
	
	
}
