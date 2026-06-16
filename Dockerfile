# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle

RUN chmod +x ./gradlew

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --version --no-daemon

COPY src ./src

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew buildFatJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=builder /app/build/libs/*-all.jar /app/app.jar

RUN useradd --uid 10001 --create-home appuser \
    && chown -R appuser:appuser /app

USER appuser

EXPOSE 8080

CMD ["java", "-jar", "/app/app.jar"]
