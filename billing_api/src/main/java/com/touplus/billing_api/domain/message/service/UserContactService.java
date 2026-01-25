package com.touplus.billing_api.domain.message.service;

import com.touplus.billing_api.common.crypto.Decrypto;
import com.touplus.billing_api.common.masking.MaskingUtils;
import com.touplus.billing_api.domain.message.dto.UserContactDto;
import com.touplus.billing_api.domain.message.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserContactService {

    private final Decrypto decrypto;

    public UserContactDto decryptAndMask(User user) {
        if (user == null) {
            return null;
        }

        return UserContactDto.builder()
                .userId(user.getUserId())
                .name(
                        MaskingUtils.maskName(
                                user.getName()
                        )
                )
                .email(
                        MaskingUtils.maskEmail(
                                decrypto.decrypt(user.getEmail())
                        )
                )
                .phone(
                        MaskingUtils.maskPhone(
                                decrypto.decrypt(user.getPhone())
                        )
                )
                .build();
    }
}
