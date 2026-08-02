# Stage 1: Build Spring Boot Application Jar
FROM eclipse-temurin:26-jdk-alpine AS builder

WORKDIR /app

# Copy Gradle wrapper and build config
COPY gradle/ gradle/
COPY gradlew build.gradle settings.gradle ./

# Ensure gradlew executable permissions
RUN chmod +x gradlew

# Copy backend source code
COPY src/ src/

# Build executable Boot JAR (excluding test execution & frontend Gradle task)
RUN ./gradlew bootJar -x test -x buildFrontend --no-daemon

# Stage 2: Production Lightweight Runtime
FROM eclipse-temurin:26-jre-alpine

WORKDIR /app

ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xms256m -Xmx512m"

# Create unprivileged user for container security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy JAR artifact from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
