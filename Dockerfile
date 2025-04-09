FROM openjdk:17-jdk-bullseye

WORKDIR /app

# Dockerize 설치
ENV DOCKERIZE_VERSION v0.2.0
RUN wget https://github.com/jwilder/dockerize/releases/download/$DOCKERIZE_VERSION/dockerize-linux-amd64-$DOCKERIZE_VERSION.tar.gz \
    && tar -C /usr/local/bin -xzvf dockerize-linux-amd64-$DOCKERIZE_VERSION.tar.gz

# jar 복사
COPY build/libs/*.jar app.jar

EXPOSE 8080

# dockerize로 MySQL이 준비될 때까지 기다리고, Spring 애플리케이션 실행
ENTRYPOINT ["dockerize", "-wait", "tcp://mysql-test:3306", "-timeout", "30s", "java", "-jar", "app.jar"]
