package com.touplus.billing_api.admin.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

import com.touplus.billing_api.admin.dto.WholeProcessDto;
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
    
    
    @Override
    public WholeProcessDto getWholeProcessStatus() {

        LocalDate settlementMonth = getLastMonth();

        long total = messageRepo.countTotal();

        long batch = batchRepo.countBatch(settlementMonth);
        long kafkaSent = batchRepo.countKafkaSent(settlementMonth);
        long kafkaReceive = messageRepo.countKafkaReceive(settlementMonth);
        long createMessage = messageRepo.countCreateMessage(settlementMonth);
        long sentMessage = messageRepo.countSentMessage(settlementMonth);

        double batchRate = rate(batch, total);
        double kafkaSentRate = rate(kafkaSent, total);
        double kafkaReceiveRate = rate(kafkaReceive, total);
        double createMessageRate = rate(createMessage, total);
        double sentMessageRate = rate(sentMessage, total);

        System.out.println("settlementMonth : " + settlementMonth);
        System.out.println("total : " + total);
        System.out.println("batch : " + batch);
        System.out.println("kafkaSent : " + kafkaSent);
        System.out.println("kafkaReceive : " + kafkaReceive);
        System.out.println("createMessage : " + createMessage);
        System.out.println("sentMessage : " + sentMessage);
        System.out.println("batchRate : " + batchRate);
        System.out.println("kafkaSentRate : " + kafkaSentRate);
        System.out.println("kafkaReceiveRate : " + kafkaReceiveRate);
        System.out.println("createMessageRate : " + createMessageRate);
        System.out.println("sentMessageRate : " + sentMessageRate);
        
        return WholeProcessDto.builder()
                .totalCount(total)
                .batchCount(batch)
                .kafkaSentCount(kafkaSent)
                .kafkaReceiveCount(kafkaReceive)
                .createMessageCount(createMessage)
                .sentMessageCount(sentMessage)
                .batchRate(batchRate)
                .kafkaSentRate(kafkaSentRate)
                .kafkaReceiveRate(kafkaReceiveRate)
                .createMessageRate(createMessageRate)
                .sentMessageRate(sentMessageRate)
                .settlementMonth(
                        settlementMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))
                )
                .build();
    }

    private double rate(long value, long total) {
        return total == 0 ? 0 : value * 100.0 / total;
    }

    private LocalDate getLastMonth() {
        return LocalDate.now()
                .minusMonths(1)
                .withDayOfMonth(1);
    }
}
