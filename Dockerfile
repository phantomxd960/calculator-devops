# Stage 1: Build stage
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Leverage Docker layer caching: resolve dependencies before copying source code
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build executable JAR
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Minimal runtime stage
FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="vijay.vignesh96000@gmail.com"
LABEL description="Calculator REST API service"

# Install curl for health checking
RUN apk add --no-cache curl

# Create non-root system user and group for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy executable jar from builder stage with appropriate ownership
COPY --from=builder --chown=appuser:appgroup /build/target/calculator-api-*.jar app.jar

# Run as non-root user
USER appuser

# Expose Spring Boot port
EXPOSE 8080

# JVM options and port environment variable
ENV SERVER_PORT=8080
ENV JAVA_OPTS="-Xms128m -Xmx256m -XX:+UseG1GC"

# Container healthcheck instruction
HEALTHCHECK --interval=10s --timeout=5s --start-period=15s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the Spring Boot application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
