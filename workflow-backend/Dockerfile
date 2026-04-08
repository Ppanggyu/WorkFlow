# -------- Build Stage --------
FROM gradle:8.2-jdk17 AS build
WORKDIR /app

# 비루트 사용자 생성
USER root
RUN useradd -ms /bin/bash gradleuser
USER gradleuser

# Gradle 캐시를 홈 디렉토리로
ENV GRADLE_USER_HOME=/home/gradleuser/.gradle

# 의존성 다운로드
COPY build.gradle settings.gradle ./
RUN gradle dependencies --no-daemon

# 소스 복사 후 bootJar 빌드
COPY src ./src
RUN gradle bootJar --no-daemon

# -------- Run Stage --------
FROM eclipse-temurin:17-jdk
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
