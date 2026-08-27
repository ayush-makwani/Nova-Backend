# syntax=docker/dockerfile:1
# ---------------------------------------------------------------------------
# Multi-stage build: Maven does the compiling, the runtime image ships only a
# JRE plus the fat jar - the build toolchain never reaches production.
# ---------------------------------------------------------------------------

# ---- build stage ----------------------------------------------------------
# Eclipse Temurin JDK 21 - matches <java.version>21</java.version> in pom.xml.
# Pinned to an exact Maven patch so the toolchain cannot drift underneath a
# rebuild; bump this deliberately rather than tracking a floating 3.9 tag.
FROM maven:3.9.16-eclipse-temurin-21 AS build
WORKDIR /build

# Fail loudly at build time if this image ever stops being JDK 21, rather than
# silently producing class files the JRE 21 runtime below cannot load.
RUN set -eux; \
    java -version; \
    mvn -version; \
    java -version 2>&1 | grep -q 'version "21' \
      || { echo "ERROR: build stage must be JDK 21"; exit 1; }

# pom first, on its own layer: dependency resolution is only re-run when the
# pom actually changes, not on every source edit.
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline

COPY src ./src
# Tests are skipped here by design - they belong in CI, where a failure is
# visible, rather than silently lengthening every image build.
RUN --mount=type=cache,target=/root/.m2 mvn -B -q clean package -DskipTests

# ---- runtime stage --------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy

# curl is only here so the compose healthcheck has something to call - this
# app has no actuator, so the check hits a real permitted endpoint instead.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Never run as root.
RUN groupadd --system nova && useradd --system --gid nova --create-home nova

WORKDIR /app
COPY --from=build /build/target/*.jar app.jar

# LOG_DIR defaults to ./logs; pre-create it so the non-root user owns it and
# a mounted volume does not land as root.
RUN mkdir -p /app/logs && chown -R nova:nova /app

USER nova
EXPOSE 8111

# MaxRAMPercentage lets the JVM size its heap from the container limit rather
# than the host's total memory, which is what it would otherwise see.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
