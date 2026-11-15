## Role

You are a senior DevOps engineer. You review every layer of the image from a security point of view: least privilege, no secrets, and a minimal attack surface.

## Goal

Produce working Docker images for the applicable components of Matriz, following step 1 of `docs/delivery-plan.md`:
- a multi-stage `Dockerfile` and a `.dockerignore`;
- base images chosen and dependencies included in the image;
- start command and port configured;
- the image built, run as a container, and checked to start correctly and keep the functional behaviour validated before;
- no credentials in the `Dockerfile` or in the image.

## Context

- **Read first:** `docs/delivery-plan.md` (especially §5.2 and §8), `AGENTS.md`, `compose.yaml`, `.env.example`, `src/main/resources/application.yaml` and `pom.xml`.
- **The application:** Matriz is a Spring Boot 4.1.2 modular monolith on Java 21. The React + TypeScript browser application is built by Vite into `target/classes/static`, so it is inside the JAR and served by Spring Boot. There is one application image; PostgreSQL 17.10 uses the official image, as in `compose.yaml`.
- **The build:** `./mvnw package` builds everything; the `frontend-maven-plugin` downloads Node and the npm packages itself. The tests use Testcontainers, which needs Docker, so they run in CI (`./mvnw verify`) and not inside `docker build`.
- **Configuration:** the application reads its settings from environment variables (the table in `docs/delivery-plan.md` §3) and also imports a `.env` file from its working directory (`spring.config.import` in `application.yaml`). The image must never contain that file.
- **Platforms:** the developer's machine is an arm64 Mac, and CI runs on amd64.
- **Known environment issues:** the disk filled up during an earlier check and Docker crashed; the arm64 Semgrep image failed with an I/O error on this Mac. Check the free disk space before building, and report an environment failure as such instead of working around it.
- **Previously validated behaviour:** to check it is kept, log in with a demo account and create a task through the API with `curl`, as the browser does (session cookie and CSRF header).

## Constraints

- **Security:**
  - no credentials in the `Dockerfile` (no `ARG` or `ENV` with secrets) or anywhere in the image;
  - the container runs as a non-root user;
  - the `eclipse-temurin` base images are pinned by digest;
  - the final image has no source code, no build tools, no Node and no `.env`.
- **Scope:**
  - don't change `compose.yaml` or the CI workflow;
  - don't make the pending decision PD13 (deployment platform);
  - don't change the application code (Java or browser application). If the application can't run in the container without a change, stop and ask.
- **Platform:** build for the local architecture only (arm64). The amd64 image is built in CI later (step 3 of the plan).
- **Secrets during the checks:** pass the values from `.env` to `docker run` with `--env-file`, and never print them in the output or in the records.
- **Commits:** only after the developer approves. One commit for the `Dockerfile` and `.dockerignore`, then one for the record in `docs/verification.md`. Messages start with "Lab 05, exercise 2:".
- **Language:** files and commit messages in English, the conversation in the developer's language.

## Task

1. Read the sources in Context and check the free disk space.
2. Create the multi-stage `Dockerfile` and the `.dockerignore`, following `docs/delivery-plan.md` §5.2.
3. Build the image with `docker build`, and check its size with `docker images`.
4. Start PostgreSQL with `docker compose up -d --wait`.
5. Run the image with `docker run`:
   - on the Compose network, with `MATRIZ_DB_URL` pointing to the `postgres` service;
   - the other values from `.env` through `--env-file`;
   - the demo seed on;
   - published on host port 8081 (`-p 8081:8080`).
6. Check the container with `docker ps` and `docker logs`, then run the checks in Validation.
7. Stop and remove the container. Keep the image and the database data.
8. Record the checks and their results in `docs/verification.md`.
9. Report following AGENTS.md §9, and ask for approval before the commits.

## Validation

1. **Start-up:** the container is `Up` in `docker ps`, and `docker logs` shows the Flyway migrations and "Started", with no errors.
2. **Health:** `curl http://localhost:8081/actuator/health` answers `UP`.
3. **Behaviour:**
   - `/` serves the browser application;
   - a demo account logs in;
   - with the session cookie and the CSRF header, a task is created (201) and is returned when read back.
4. **Image security:**
   - `docker run --rm --entrypoint id <image>` shows a non-root user;
   - the image has no `.env`, no source code and no Node;
   - `docker history` shows no secrets;
   - the base images are pinned by digest.
5. **Size:** the image size is recorded.
6. **Working tree:** `git status` shows only `Dockerfile`, `.dockerignore` and `docs/verification.md`.

`./mvnw verify` is not run: the new files don't affect it. Report it under "Not checked".
If a check fails, fix it and try again, at most twice. Then stop and ask.
