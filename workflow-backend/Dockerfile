# Build stage
FROM gradle:8.2-jdk17 AS build
WORKDIR /app
COPY build.gradle settings.gradle ./
RUN gradle dependencies --no-daemon
COPY src ./src
RUN gradle bootJar --no-daemon

# Run stage
FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
