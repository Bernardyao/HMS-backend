# syntax=docker/dockerfile:1
# Multi-stage build with in-container compilation and BuildKit optimization
# Build with: DOCKER_BUILDKIT=1 docker build -t his-app .
# Or with Compose: DOCKER_BUILDKIT=1 docker-compose build

# ============================================================
# Stage 1: Builder - Compile application in container
# ============================================================
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /app

# Step 1: Copy build scripts and dependency configuration first
COPY gradlew .
COPY gradle gradle
COPY build.gradle .

# Step 2: Download dependencies (utilizes BuildKit cache mount)
# This cache persists across builds, significantly speeding up dependency resolution
RUN --mount=type=cache,target=/root/.gradle \
    chmod +x gradlew && \
    ./gradlew dependencies --no-daemon \
    -Dorg.gradle.jvmargs="-Xmx512m -XX:MaxMetaspaceSize=256m"

# Step 3: Copy source code
COPY src src

# Step 4: Build application
# Using cache mount for Gradle home to speed up incremental builds
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew assemble -x test --no-daemon \
    -Dorg.gradle.jvmargs="-Xmx512m -XX:MaxMetaspaceSize=256m" \
    -Dorg.gradle.daemon=false \
    -Dorg.gradle.parallel=false

# Step 5: Extract layers for optimized Docker layering
# This splits the fat JAR into dependencies, loader, and application layers
RUN JAR=$(ls build/libs/*.jar | grep -v plain | head -1) && \
    java -Djarmode=layertools -jar "$JAR" extract --destination extracted

# ============================================================
# Stage 2: Runtime - Minimal JRE image
# ============================================================
FROM eclipse-temurin:17-jre

WORKDIR /app

# Set timezone to Asia/Shanghai
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Install curl for health checks and create non-root user
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/* && \
    addgroup --system --gid 1001 javauser && \
    adduser --system --uid 1001 --ingroup javauser --shell /bin/false javauser

# Copy extracted layers from builder stage
# Copying in order of least-frequently changed to most-frequently changed
COPY --from=builder /app/extracted/dependencies/ ./
COPY --from=builder /app/extracted/spring-boot-loader/ ./
COPY --from=builder /app/extracted/snapshot-dependencies/ ./
COPY --from=builder /app/extracted/application/ ./

# Create logs directory and set permissions
RUN mkdir -p /app/logs && \
    chown -R javauser:javauser /app

# Switch to non-root user
USER javauser

# Expose application port
EXPOSE 8080

# Health check configuration
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM Memory Configuration using percentage (adaptive)
# Using JarLauncher to start the application from extracted layers
ENTRYPOINT ["java", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:InitialRAMPercentage=50.0", \
    "-XX:+UseG1GC", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=prod", \
    "org.springframework.boot.loader.launch.JarLauncher"]
