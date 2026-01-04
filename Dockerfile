# Multi-stage build with in-container compilation and BuildKit optimization
# Build with: DOCKER_BUILDKIT=1 docker build -t his-app .
# Or with Compose: DOCKER_BUILDKIT=1 docker-compose build

# ============================================================
# Stage 1: Builder - Compile application in container
# ============================================================
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /app

# Step 1: Copy build scripts and dependency configuration first
# This layer will be cached unless build.gradle or gradle files change
COPY gradlew .
COPY gradle gradle
COPY build.gradle .

# Step 2: Download dependencies (utilizes Docker layer cache)
# This only re-runs if dependency configuration changes
# 限制内存使用避免 OOM
RUN chmod +x gradlew && \
    ./gradlew dependencies --no-daemon \
    -Dorg.gradle.jvmargs="-Xmx512m -XX:MaxMetaspaceSize=256m"

# Step 3: Copy source code
# This layer is invalidated when source code changes
COPY src src

# Step 4: Build application
# 跳过测试和代码质量检查以加快构建
# -x test: Skip tests
# -x checkstyleMain: Skip checkstyle checks
# -x pmdMain: Skip PMD checks
RUN ./gradlew assemble -x test --no-daemon \
    -Dorg.gradle.jvmargs="-Xmx512m -XX:MaxMetaspaceSize=256m" \
    -Dorg.gradle.daemon=false \
    -Dorg.gradle.parallel=false

# Step 5: Extract JAR layers for better caching
# Spring Boot layered JARs allow more efficient Docker layer caching
# 找到不含 "plain" 的 JAR 文件（Spring Boot fat JAR）
RUN JAR=$(ls build/libs/*.jar | grep -v plain | head -1) && \
    java -Djarmode=layertools -jar "$JAR" extract

# ============================================================
# Stage 2: Runtime - Minimal JRE image
# ============================================================
FROM eclipse-temurin:17-jre

WORKDIR /app

# Install curl for health checks
RUN apt-get update && \
    apt-get install -y curl && \
    rm -rf /var/lib/apt/lists/*

# Create non-root user for security
RUN addgroup --system --gid 1001 javauser && \
    adduser --system --uid 1001 --ingroup javauser --shell /bin/false javauser

# Copy extracted layers from builder stage
COPY --from=builder /app/dependencies/ ./
COPY --from=builder /app/spring-boot-loader/ ./
COPY --from=builder /app/snapshot-dependencies/ ./
COPY --from=builder /app/application/ ./

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
# -XX:MaxRAMPercentage=75.0: Use up to 75% of container memory limit
# -XX:InitialRAMPercentage=50.0: Start with 50% of container memory limit
# -XX:+UseG1GC: Use G1 garbage collector for better memory management
# Can be overridden via JAVA_OPTS environment variable
ENTRYPOINT ["java", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:InitialRAMPercentage=50.0", \
    "-XX:+UseG1GC", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=prod", \
    "org.springframework.boot.loader.JarLauncher"]
