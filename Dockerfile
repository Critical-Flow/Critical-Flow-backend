# ─── Stage 1: 빌드 ──────────────────────────────────────────────
# JDK 17이 포함된 경량 이미지로 JAR 파일을 빌드합니다
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# 의존성 캐시 레이어 (소스 변경과 별도로 캐싱됨)
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon

# 소스 코드 복사 후 빌드 (테스트 제외 — CI에서 이미 통과)
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# ─── Stage 2: 실행 ──────────────────────────────────────────────
# JRE만 포함된 경량 이미지로 실행 (빌드 도구 제외, 이미지 크기 절감)
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 보안: root 대신 전용 유저로 실행
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

# 빌드 결과물(JAR)만 복사
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
