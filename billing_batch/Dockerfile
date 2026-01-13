FROM gradle:jdk17-alpine AS builder
WORKDIR /workspace
COPY . .
RUN chmod +x ./gradlew
RUN ./gradlew :billing_batch:bootJar -x test

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /workspace/billing_batch/build/libs/*.jar app.jar
CMD ["java", "-jar", "app.jar"]
