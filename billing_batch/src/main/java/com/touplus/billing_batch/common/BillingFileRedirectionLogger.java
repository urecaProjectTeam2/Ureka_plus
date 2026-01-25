package com.touplus.billing_batch.common;

import org.springframework.stereotype.Component;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class BillingFileRedirectionLogger {
    private static final String LOG_DIR = "logs/billing_audit";
    private BufferedWriter writer;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public synchronized void init() {
        try {
            Files.createDirectories(Paths.get(LOG_DIR));
            String fileName = LOG_DIR + "/billing_exec_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".log";
            writer = new BufferedWriter(new FileWriter(fileName, true));
            write("========== 정산 배치 파일 리다이렉션 시작 ==========");
        } catch (IOException e) {
            System.err.println("파일 리다이렉션 로그 생성 실패: " + e.getMessage());
        }
    }

    public synchronized void write(String message) {
        if (writer == null) return;
        try {
            writer.write(String.format("[%s] [%s] %s%n",
                    LocalDateTime.now().format(formatter),
                    Thread.currentThread().getName(), // 어떤 스레드에서 썼는지 기록
                    message));
            writer.flush(); // 매번 디스크로 밀어내어 의도적 지연 발생
        } catch (IOException ignored) {}
    }

    public synchronized void close() {
        try {
            if (writer != null) {
                write("========== 정산 배치 파일 리다이렉션 종료 ==========");
                writer.close();
                writer = null;
            }
        } catch (IOException ignored) {}
    }
}