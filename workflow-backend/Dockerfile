# -------- Build Stage --------
FROM gradle:8.2-jdk17 AS build
WORKDIR /app

# Gradle 캐시를 프로젝트 내부로 지정
ENV GRADLE_USER_HOME=/app/.gradle

# 의존성만 먼저 다운로드
COPY build.gradle settings.gradle ./
RUN gradle dependencies --no-daemon

# 소스 복사 후 bootJar 빌드
COPY src ./src
RUN gradle bootJar --no-daemon

# -------- Run Stage --------
FROM eclipse-temurin:17-jdk
WORKDIR /app

# 빌드된 jar 복사
COPY --from=build /app/build/libs/*.jar app.jar

# React 정적 파일 빌드 후 /app/static에 복사 가능 (선택)
# COPY --from=frontend-build /app/build ./static

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
