# Gradle 이미지에서 빌드
FROM gradle:8.2-jdk17 AS build
WORKDIR /app

COPY build.gradle settings.gradle ./
RUN gradle build --no-daemon || true  # 의존성 캐시용
COPY src ./src
RUN gradle bootJar --no-daemon

# 실행용 JDK 이미지
FROM eclipse-temurin:17-jdk
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
