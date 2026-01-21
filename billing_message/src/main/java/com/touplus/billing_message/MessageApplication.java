package com.touplus.billing_message;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class MessageApplication {

    public static void main(String[] args) {
        // 1. .env 파일 로드
       /* Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing() // 파일이 없어도 에러 내지 않음
                .load();

        // 2. 로드된 변수들을 시스템 프로퍼티로 등록 (스프링이 ${}로 읽을 수 있게 함)
        dotenv.entries().forEach(entry -> {
            System.setProperty(entry.getKey(), entry.getValue());
        });*/

        SpringApplication.run(MessageApplication.class, args);
    }

}
