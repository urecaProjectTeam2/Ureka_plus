FROM gradle:jdk17-alpine AS builder
WORKDIR /workspace
# 전체 프로젝트 코드를 복사 (멀티 모듈 의존성 해결을 위해)
COPY . .
# gradlew 실행 권한 부여 (Linux 컨테이너에서 필요)
RUN chmod +x ./gradlew
# API 모듈만 빌드 (테스트는 켬/끔 선택, 일단 생략하여 속도 향상)
RUN ./gradlew :billing_api:bootJar -x test

FROM eclipse-temurin:17-jre
WORKDIR /app
# 빌드된 JAR 파일만 가져오기
COPY --from=builder /workspace/billing_api/build/libs/*.jar app.jar
CMD ["java", "-jar", "app.jar"]
