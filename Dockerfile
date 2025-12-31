# Multi-stage build for PartyTime Server
# Stage 1: Build the application
FROM gradle:8.11-jdk25-alpine AS builder

WORKDIR /app

# Copy gradle wrapper and build files
COPY gradlew gradlew.bat ./
COPY gradle ./gradle
COPY build.gradle settings.gradle ./

# Download dependencies (this layer is cached unless build files change)
RUN ./gradlew dependencies --no-daemon || true

# Copy source code
COPY src ./src

# Build the application JAR
RUN ./gradlew bootJar --no-daemon

# Stage 2: Create the runtime image
FROM azul/zulu-openjdk-alpine:25-jre

WORKDIR /app

# Create a non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Copy the built JAR from the builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Set ownership to non-root user
RUN chown -R spring:spring /app

# Switch to non-root user
USER spring

# Expose the application port
EXPOSE 8000

# JVM optimizations for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
