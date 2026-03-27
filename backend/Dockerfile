# Build — Gradle from image (avoids missing gradle-wrapper.jar in git / Docker context)
FROM gradle:8.14.2-jdk21 AS builder
WORKDIR /workspace

COPY build.gradle settings.gradle ./
COPY src src

RUN gradle bootJar --no-daemon -x test

# Run (smaller JRE image)
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=builder /workspace/build/libs/*.jar /app/app.jar
COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

# Render injects PORT; application.properties uses server.port=${PORT:8080}
ENV PORT=8080
ENV SPRING_PROFILES_ACTIVE=render
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"
EXPOSE 8080

ENTRYPOINT ["/docker-entrypoint.sh"]
