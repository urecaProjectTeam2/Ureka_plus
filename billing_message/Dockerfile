FROM gradle:jdk17-alpine AS builder
WORKDIR /workspace
COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew :billing_message:bootJar -x test

FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=builder /workspace/billing_message/build/libs/*.jar app.jar
CMD ["java", "-jar", "app.jar"]
