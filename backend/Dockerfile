# syntax=docker/dockerfile:1

# ---- build: JDK 25, the same toolchain build.gradle asks for --------------------------------------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

# Dependencies first, so source changes don't invalidate the dependency layer.
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN ./gradlew --no-daemon dependencies > /dev/null

COPY src src
# Tests run in CI and locally (`./gradlew check`), not in the image build: the ITs need Docker.
RUN ./gradlew --no-daemon bootJar

# ---- runtime: JRE only, non-root ------------------------------------------------------------------
FROM eclipse-temurin:25-jre

# curl is only for the container healthcheck.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 --no-create-home spring

WORKDIR /app
COPY --from=build /workspace/build/libs/app.jar app.jar

USER spring
EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=3s --start-period=40s --retries=5 \
    CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
