# 1단계: Build Stage
FROM gradle:7.6.3-jdk17 AS builder

WORKDIR /app
COPY . /app

# Gradle 캐시 활용을 위한 의존성 사전 빌드 (선택사항)
# RUN gradle dependencies

RUN ./gradlew build --exclude-task test

# 2단계: Run Stage
FROM openjdk:17-jdk-slim

WORKDIR /app

# 빌드된 jar 파일만 복사
COPY --from=builder /app/build/libs/graduationExhibitions-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
