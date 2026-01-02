# Build stage
FROM gradle:8.5-jdk17 AS build
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY gradle gradle
COPY src src
# Copy common libraries if they exist (for CI/CD builds)
# Note: In CI/CD, common/sns-lib is copied before Docker build
COPY common common
RUN gradle build --no-daemon -x test

# Runtime stage
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
