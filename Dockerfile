# syntax=docker/dockerfile:1

# The Matriz application image (docs/delivery-plan.md, section 5.2): one JAR with the browser
# application inside, served by Spring Boot. PostgreSQL runs in its own container.
#
# Build:  docker build -t matriz .
# Run:    docker run -p 8080:8080 -e MATRIZ_DB_URL=… -e MATRIZ_DB_USER=… -e MATRIZ_DB_PASSWORD=… matriz
# Every setting, secrets included, comes from environment variables at run time (docs/delivery-plan.md,
# section 3). None is set here, and .env is never copied in (.dockerignore).
#
# Base images: Eclipse Temurin 21.0.13_9 on Ubuntu 24.04, pinned by the digest of the multi-platform
# index, so the same pin works on arm64 and amd64.

# Build stage: the JDK, Maven (through the wrapper) and Node (downloaded by the frontend-maven-plugin).
# None of it reaches the final image.
FROM eclipse-temurin:21.0.13_9-jdk-noble@sha256:06c1f157dacf6b68198bd9a4186dd4c34ef24e65d7bbbe2a0cf8e62571ad38c9 AS build
WORKDIR /workspace

COPY mvnw pom.xml ./
COPY .mvn/wrapper/maven-wrapper.properties .mvn/wrapper/
COPY frontend frontend
COPY src/main src/main

# The tests aren't run or compiled here: they need Docker (Testcontainers) and run in CI with
# ./mvnw verify. The Maven and npm caches last between builds but are never part of a layer.
RUN --mount=type=cache,target=/root/.m2 \
    --mount=type=cache,target=/root/.npm \
    ./mvnw --batch-mode --no-transfer-progress package -DskipTests -Dmaven.test.skip=true \
    && cp target/matriz-*.jar /workspace/matriz.jar

# Final stage: only the Java runtime and the JAR.
FROM eclipse-temurin:21.0.13_9-jre-noble@sha256:0d57a8e90c2bb844c5a8e1b2b71e11eff06dad7edcfd803cd21ed4ae203b2515

# Ubuntu security updates published after the base image was built, so a known vulnerability that
# already has a fix doesn't wait for a new base image (the image scan in CI fails on those).
RUN apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get upgrade --yes --no-install-recommends \
    && rm -rf /var/lib/apt/lists/*

# A system user with no home and no login shell runs the application.
RUN groupadd --system --gid 10001 matriz \
    && useradd --system --uid 10001 --gid matriz --no-create-home --shell /usr/sbin/nologin matriz

# The JAR belongs to root and can only be read by the application user, which can't write to /app.
# So no .env can be added to the working directory at run time either.
WORKDIR /app
COPY --from=build --chown=root:root --chmod=0444 /workspace/matriz.jar /app/matriz.jar

USER 10001:10001
EXPOSE 8080

# Marks the container healthy once the application answers. curl comes with the base image. The port
# is the one inside the container (SERVER_PORT, 8080 unless changed).
HEALTHCHECK --interval=10s --timeout=3s --start-period=60s --retries=3 \
    CMD curl --fail --silent --output /dev/null "http://localhost:${SERVER_PORT:-8080}/actuator/health" || exit 1

ENTRYPOINT ["java", "-jar", "/app/matriz.jar"]
