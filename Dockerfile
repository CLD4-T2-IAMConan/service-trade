# Build stage
FROM gradle:8.5-jdk17 AS build
WORKDIR /app

# Copy common/sns-lib first and publish to Maven local
# Note: In CI/CD, common/sns-lib should be copied to build context before building
# If common/sns-lib doesn't exist in build context, this COPY will fail
# For local builds, ensure common/sns-lib exists or comment out these lines
COPY common/sns-lib common/sns-lib
WORKDIR /app/common/sns-lib
RUN gradle publishToMavenLocal --no-daemon

# Build service-trade
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY gradle gradle
COPY src src
RUN gradle build --no-daemon -x test

# Runtime stage
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
