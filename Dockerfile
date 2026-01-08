# Build stage
FROM gradle:8.5-jdk17 AS build
WORKDIR /app

# Copy common/sns-lib first and publish to Maven local
COPY common/sns-lib common/sns-lib
WORKDIR /app/common/sns-lib
# Ensure settings.gradle exists (it should be copied with the directory)
RUN gradle publishToMavenLocal --no-daemon

# Build service-trade
WORKDIR /app
COPY service-trade/build.gradle service-trade/settings.gradle ./
COPY service-trade/gradle gradle
COPY service-trade/src src
RUN gradle build --no-daemon -x test

# Runtime stage
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
