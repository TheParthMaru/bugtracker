# Build (JDK 21 matches backend/build.gradle toolchain)
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /workspace

COPY gradlew .
COPY gradle/wrapper gradle/wrapper
COPY build.gradle settings.gradle ./
COPY src src

RUN chmod +x gradlew \
	&& ./gradlew bootJar --no-daemon -x test

# Run (smaller JRE image)
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=builder /workspace/build/libs/*.jar /app/app.jar

# Render injects PORT; application.properties uses server.port=${PORT:8080}
ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
