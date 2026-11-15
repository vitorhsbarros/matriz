## Role

You are a senior site reliability engineer. You make the application easy to operate:
- logs that a machine can parse and a person can read;
- health checks that tell "alive" from "ready to serve".

You never let personal data or secrets reach a log or a public endpoint.

## Goal

Make Matriz easier to operate, as Challenge B asks (observability):
- **Structured logs:**
  - in the container image, the application writes its logs as JSON in the Elastic Common Schema (ECS) format, through Spring Boot's built-in structured logging;
  - a local run outside the container keeps the plain-text logs.
- **Extra availability checks:**
  - `/actuator/health/liveness` and `/actuator/health/readiness` become public, without details, like `/actuator/health`;
  - readiness includes the database, so the application reports "not ready" while PostgreSQL can't be reached;
  - liveness doesn't include it, so a database failure doesn't get the application restarted;
  - the image's health check uses readiness, and the CI smoke test checks both.

## Context

- **Read first:** `src/main/resources/application.yaml`, `src/main/java/matriz/web/internal/SecurityConfig.java`, the `Dockerfile`, `compose.yaml`, `scripts/image-smoke-test.sh`, `.github/workflows/ci.yml`, `docs/constraints.md` (the SEC and PRI rules), `docs/architecture.md`, `docs/verification.md` (§14 to §17) and `AGENTS.md`.
- **Logs today:** there is no logging configuration. The application writes Spring Boot's default plain-text logs.
- **Health today:**
  - only the `health` endpoint is exposed (`management.endpoints.web.exposure.include: health`);
  - `/actuator/health` is public (`SecurityConfig`) and already shows the groups `liveness` and `readiness`;
  - `/actuator/health/liveness` and `/actuator/health/readiness` answer 401, because `SecurityConfig` lets only `/actuator/health` through.
- **What uses health today:**
  - the image's `HEALTHCHECK`;
  - `docker compose --wait`, through that health check;
  - `scripts/image-smoke-test.sh`, locally and in the CI `image` job.
- **Version:** Spring Boot 4.1.2, which has structured logging built in (`logging.structured.format.console`: `ecs`, `logstash`, `gelf`). No new dependency is needed.
- **Tests:** `./mvnw verify` runs the back-end tests against PostgreSQL in Testcontainers, the browser application tests, `ModularityTest` and the end-to-end test. The API tests send real HTTP requests to the application on a random port.
- **Environment:**
  - a local `java -jar` of the application runs on port 8081, and port 8080 is taken by another program. Leave both alone;
  - the Compose checks use `MATRIZ_APP_PORT=8082`.

## Constraints

- **Security and privacy:**
  - the probes show only their status, never details or components (`show-details` stays as it is);
  - `SecurityConfig` lets through only `/actuator/health/liveness` and `/actuator/health/readiness`, besides what it already allows. No other actuator endpoint is exposed or opened. This prompt names that change, so running it approves it (AGENTS.md §6);
  - no log line may contain a password, a session ID, a CSRF token, a request body or other personal data (SEC and PRI in `docs/constraints.md`).
- **Personal data in today's logs:** review the log statements in `src/main/java` and the log output of a log-in, a failed log-in and an error. Report anything that prints personal data or secrets. Fix it only if it is part of this change; otherwise report it and ask.
- **No new dependencies.** Only Spring Boot's built-in structured logging and Actuator.
- **Local runs:** a run outside the container (`java -jar`, `./mvnw spring-boot:run`) keeps the plain-text logs.
- **Scope:**
  - request logging and correlation IDs are out of scope;
  - README.md and AGENTS.md are left for step 5 of the delivery plan;
  - don't make PD13.
- **Data:** never remove the `matriz-postgres` volume or its data.
- **Commits:** only after the developer approves. The change (configuration, security, image, smoke test, tests) in one commit, the record in `docs/verification.md` in another. Messages start with "Lab 05, challenge B:". The push to `main` also needs approval.
- **Language:** code, files and commit messages in English, the conversation in the developer's language.

## Task

1. Read the sources in Context.
2. Review the current log statements and log output for personal data and secrets (see Constraints), and note the findings.
3. In `application.yaml`:
   - enable the health probes explicitly;
   - make the `readiness` group `readinessState` and `db`, and the `liveness` group `livenessState` only.
4. In `SecurityConfig`, allow `/actuator/health/liveness` and `/actuator/health/readiness` without a session.
5. Add an integration test, in the style of the existing API tests:
   - without a session, both probes answer 200 with `{"status":"UP"}` and no details;
   - another actuator path (e.g. `/actuator/info`, `/actuator/env`) is still not reachable.
6. In the `Dockerfile`:
   - set `LOGGING_STRUCTURED_FORMAT_CONSOLE=ecs` as an environment variable of the final stage;
   - point the `HEALTHCHECK` at `/actuator/health/readiness`.
7. In `scripts/image-smoke-test.sh`, add checks that:
   - both probes answer `UP`;
   - the application container's log lines are JSON with the ECS fields `@timestamp`, `log.level`, `message`, `service.name` and `ecs.version`.
8. Run `./mvnw verify`.
9. Build the image and start it with `docker compose --profile app up -d --wait`, then run the smoke test.
10. **Database down:** stop the `postgres` service while `app` runs. Check that readiness reports `DOWN` (503) while liveness stays `UP` (200), and that the container becomes unhealthy. Start `postgres` again, and check that readiness goes back to `UP`. Check the logs of that period for leaked data.
11. Leave things as they were: remove the `app` service, and keep `postgres` running with its data.
12. Record the checks in `docs/verification.md`.
13. Report following AGENTS.md §9, and ask for approval before the commits and the push.

## Validation

1. **Tests:** `./mvnw verify` passes, the new test included.
2. **Probes:** without a session, `/actuator/health/liveness` and `/actuator/health/readiness` answer 200 and `{"status":"UP"}`, with no `components` or `details`. Other actuator paths are still not reachable.
3. **Database down:** readiness answers 503 `DOWN`, liveness stays 200 `UP`, and the container turns `unhealthy`. After PostgreSQL is back, readiness is `UP` again.
4. **Logs:**
   - in the container, every application log line parses as JSON with the ECS fields;
   - a local `java -jar` still writes plain text;
   - no log line seen during the checks (log-in, failed log-in, task creation, database down) contains a password, session ID, CSRF token, request body or email address, unless reported under step 2.
5. **Smoke test:** passes with the new checks, and fails, naming the check, when the logs aren't JSON (e.g. run against an image without the variable).
6. **Working tree:** only `application.yaml`, `SecurityConfig.java`, the new test, the `Dockerfile`, `scripts/image-smoke-test.sh`, `docs/verification.md` and the prompt changed.

If a check fails, fix it and try again, at most twice. Then stop and ask.
