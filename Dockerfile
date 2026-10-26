# syntax=docker/dockerfile:1
# ------------------------------------------------------------------------------
# Build Stage
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /build

# Copy Maven wrapper and POM first for layer caching
COPY pom.xml mvnw ./
COPY .mvn .mvn

# Make wrapper executable
RUN chmod +x ./mvnw

# Copy source code and build executable jar
COPY src src
RUN ./mvnw clean package -DskipTests -B

# ------------------------------------------------------------------------------
# Runtime Stage
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Install curl for healthcheck
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Copy built application jar from builder stage
COPY --from=builder /build/target/waymate-service-*.jar /app/waymate-service.jar

# Configuration defaults
ENV SPRING_PROFILES_ACTIVE=local \
    SERVER_PORT=8781 \
    SERVER_ADDRESS=0.0.0.0 \
    NEO4J_HOST=host.docker.internal \
    NEO4J_PORT=7687 \
    NEO4J_USERNAME=neo4j \
    NEO4J_PASSWORD=!Honda_cbr650r \
    AI_AGENT_URL=http://waymate-agent:8000

EXPOSE 8781

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8781/api/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/waymate-service.jar"]
