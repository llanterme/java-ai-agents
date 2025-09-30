# Multi-stage build for optimized Java Spring Boot application
FROM maven:3.9-eclipse-temurin-17 AS builder

# Set working directory
WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

# Add non-root user for security
RUN addgroup -g 1000 -S spring && \
    adduser -u 1000 -S spring -G spring

# Install curl for health checks
RUN apk add --no-cache curl

# Set working directory
WORKDIR /app

# Create directories for generated images with proper permissions
RUN mkdir -p /app/generated-images && \
    chown -R spring:spring /app

# Copy JAR from builder stage
COPY --from=builder --chown=spring:spring /app/target/*.jar app.jar

# Switch to non-root user
USER spring:spring

# Expose port
EXPOSE 9001

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM memory optimization for containers
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 \
    -XX:InitialRAMPercentage=50 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -Djava.security.egd=file:/dev/./urandom"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]