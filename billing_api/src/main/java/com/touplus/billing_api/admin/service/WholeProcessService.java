package com.touplus.billing_api.admin.service;

import com.touplus.billing_api.admin.dto.WholeProcessDto;
import com.touplus.billing_api.admin.entity.BatchProcessEntity;
import com.touplus.billing_api.admin.entity.MessageProcessEntity;

public interface WholeProcessService {

   BatchProcessEntity getBatchStatus();

   MessageProcessEntity getMessageStatus();
   

   WholeProcessDto getWholeProcessStatus();
}
