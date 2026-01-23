package com.touplus.billing_message.common.crypto;

import com.macasaet.fernet.Key;
import com.macasaet.fernet.Token;
import com.macasaet.fernet.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Slf4j
@Component
public class Decrypto {

    private final Key fernetKey;
    private final Validator validator;

    public Decrypto(@Value("${crypto.fernet.key}") String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalArgumentException("암호화 키(crypto.fernet.key)가 설정되지 않았습니다.");
        }
        try {
            byte[] decodedKey = Base64.getUrlDecoder().decode(base64Key);
            this.fernetKey = new Key(decodedKey);
        }catch (IllegalArgumentException e){
            throw new RuntimeException("잘못된 Fernet 키 형식입니다. 설정을 확인하세요.", e);
        }
        this.validator = new StringValidator();
    }

    /**
     * Fernet 복호화
     *
     * @param encryptedValue Fernet 암호화된 문자열
     * @return 복호화된 문자열
     */
    public String decrypt(String encryptedValue) {
        if (encryptedValue == null || encryptedValue.isBlank()) {
            return encryptedValue;
        }

        try {
            Token token = Token.fromString(encryptedValue);

            return (String) token.validateAndDecrypt(fernetKey, validator);
        } catch (Exception e) {
            // 복호화에 실패했을 때 원본을 보여줄지, 빈 값을 보여줄지 정책에 따라 결정
            // 로그를 남겨서 어떤 데이터가 문제인지 파악할 수 있게 함.
            log.error("복호화 실패: {}", e.getMessage());
            return encryptedValue;
        }
    }
}