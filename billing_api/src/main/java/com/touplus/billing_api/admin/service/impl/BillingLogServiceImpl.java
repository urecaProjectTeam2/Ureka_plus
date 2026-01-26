package com.touplus.billing_api.admin.service.impl;

import com.touplus.billing_api.admin.service.BillingLogService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class BillingLogServiceImpl implements BillingLogService {

    private static final String LOG_DIR = "./logs/billing_audit";

    @Value("${billing.log.dir}")
    private String logDir;

    @Override
    public List<String> getLogFileList() {
        Path rootPath = Paths.get(logDir).toAbsolutePath().normalize();
        File dir = rootPath.toFile();

        System.out.println("DEBUG: 로그 경로 존재 여부 = " + dir.exists());
        System.out.println("DEBUG: 로그 경로 절대 주소 = " + dir.getAbsolutePath());
        if (!dir.exists() || !dir.isDirectory()) return Collections.emptyList();

        if (!dir.exists() || !dir.isDirectory()) {
            System.out.println("DEBUG: 경로가 없거나 디렉토리가 아닙니다.");
            return Collections.emptyList();
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(".log"));
        if (files == null || files.length == 0) {
            System.out.println("DEBUG: 폴더 안에 .log 파일이 하나도 없습니다.");
            return Collections.emptyList();
        }

        System.out.println("DEBUG: 발견된 파일 개수 = " + files.length);

        return Arrays.stream(files)
                .sorted((f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()))
                .map(File::getName)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> readLogFile(String fileName) {
        if (isInvalidPath(fileName)) return Collections.singletonList("잘못된 파일명입니다.");

        Path filePath = Paths.get(LOG_DIR).resolve(fileName).normalize();
        try (Stream<String> lines = Files.lines(filePath, StandardCharsets.UTF_8)) {
            return lines.collect(Collectors.toList());
        } catch (IOException e) {
            return Collections.singletonList("로그 파일을 읽을 수 없습니다: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<Resource> downloadLogFile(String fileName) {
        if (isInvalidPath(fileName)) return ResponseEntity.badRequest().build();

        try {
            Path filePath = Paths.get(LOG_DIR).resolve(fileName).normalize();
            Resource resource = new FileSystemResource(filePath.toFile());
            if (!resource.exists()) return ResponseEntity.notFound().build();

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private boolean isInvalidPath(String fileName) {
        return fileName.contains("..") || fileName.contains("/") || fileName.contains("\\");
    }

    @Override
    public boolean checkErrorInFile(String fileName) {
        // 모든 메서드에서 동일하게 Paths.get(LOG_DIR)을 사용해야 합니다.
        Path path = Paths.get(LOG_DIR).resolve(fileName).toAbsolutePath().normalize();
        if (!Files.exists(path)) return false;

        try (Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8)) {
            return lines.anyMatch(line -> line.contains("ERROR") || line.contains("FAIL"));
        } catch (IOException e) {
            return false;
        }
    }
    @PostConstruct
    public void ensureLogDirectoryExists() {
        Path logPath = Paths.get(logDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(logPath);
            System.out.println("DEBUG: 로그 디렉토리 보장됨 = " + logPath);
        } catch (IOException e) {
            throw new IllegalStateException("로그 디렉토리 생성 실패: " + logPath, e);
        }
    }

}