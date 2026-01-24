package com.touplus.billing_api.admin.service.impl;

import org.springframework.stereotype.Service;

import com.touplus.billing_api.admin.entity.BatchProcessEntity;
import com.touplus.billing_api.admin.entity.MessageProcessEntity;
import com.touplus.billing_api.admin.enums.ProcessType;
import com.touplus.billing_api.admin.repository.BatchProcessRepository;
import com.touplus.billing_api.admin.repository.MessageProcessRepository;
import com.touplus.billing_api.admin.service.WholeProcessService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WholeProcessServiceImpl implements WholeProcessService {

    private final BatchProcessRepository batchRepo;
    private final MessageProcessRepository messageRepo;

    @Override
    public BatchProcessEntity getBatchStatus() {
        BatchProcessEntity batch = new BatchProcessEntity();

        batch.setJob(
            batchRepo.findLatestJobStatus() != null
                ? batchRepo.findLatestJobStatus()
                : ProcessType.WAITED
        );

        batch.setKafkaSent(
            batchRepo.findLatestKafkaSentStatus() != null
                ? batchRepo.findLatestKafkaSentStatus()
                : ProcessType.WAITED
        );

        return batch;
    }
    
    @Override
    public MessageProcessEntity getMessageStatus() {
        MessageProcessEntity message = new MessageProcessEntity();

        message.setKafkaReceive(
            messageRepo.findLatestKafkaReceiveStatus() != null
                ? messageRepo.findLatestKafkaReceiveStatus()
                : ProcessType.WAITED
        );

        message.setCreateMessage(
            messageRepo.findLatestCreateMessageStatus() != null
                ? messageRepo.findLatestCreateMessageStatus()
                : ProcessType.WAITED
        );

        message.setSentMessage(
            messageRepo.findLatestSentMessageStatus() != null
                ? messageRepo.findLatestSentMessageStatus()
                : ProcessType.WAITED
        );

        return message;
    }
}
