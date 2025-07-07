# Stage 1: Build the Spring Boot application
FROM eclipse-temurin:21 AS build
WORKDIR /workspace
COPY ./gradlew ./gradlew.bat ./settings.gradle.kts ./build.gradle.kts ./
COPY ./gradle ./gradle
COPY ./src ./src

# Ensure Gradle wrapper is executable
RUN chmod +x gradlew
# Build the fat JAR
RUN ./gradlew bootJar --no-daemon

# Stage 2: Run the application
FROM eclipse-temurin:21-alpine
WORKDIR /app
# Copy the fat JAR from the build stage
COPY --from=build /workspace/build/libs/*.jar app.jar

# Expose application port
EXPOSE 8080

# Entry point
ENTRYPOINT ["java", "-jar", "app.jar"]
