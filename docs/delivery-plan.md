# Matriz — Delivery plan

The technical plan for delivering Matriz with Docker, Docker Compose and GitHub Actions. It is based on an analysis of the repository on 2026-11-15 (commit `ef82c80`, release `v1.0.1`), and was approved by the user on the same day.

The deployment platform is still a pending decision (PD13, [constraints.md](constraints.md)). This plan doesn't choose one. The parts that depend on it are marked **PD13**.

## 1. Components

| Component | What it is | Evidence |
|---|---|---|
| Application | A Spring Boot 4.1.2 modular monolith, Java 21, built as one JAR (`matriz-0.1.0-SNAPSHOT.jar`) | `pom.xml` (parent, `java.version`, `version`) |
| Modules | `web`, `accounts`, `projects`, `teams`, `tasks`, checked by `ModularityTest` | `src/main/java/matriz/` |
| Browser application | React + TypeScript built with Vite into `target/classes/static`, so it is inside the JAR and served by Spring Boot | `frontend/vite.config.ts` (`outDir`) |
| Database | PostgreSQL 17.10. Flyway creates the schema at start-up: six migrations, one folder per module | `compose.yaml`, `src/main/resources/db/migration/` |
| Sessions | Spring Session JDBC, stored in the same database. The cookie is `Secure` by default | `application.yaml` (`spring.session`, `MATRIZ_COOKIE_SECURE`) |
| Public endpoints | `/`, the static files, `/actuator/health` and `POST /api/session` | `SecurityConfig.java` |
| CI | GitHub Actions: build and tests, Semgrep, CodeQL, OSV-Scanner over a CycloneDX SBOM. No image and no deployment yet | `.github/workflows/ci.yml` |

## 2. Dependencies, versions and commands

- **To build:** JDK 21 and the Maven wrapper. The `frontend-maven-plugin` 2.0.2 downloads Node v24.22.3 and the npm packages itself. The build needs network access to Maven Central, nodejs.org and the npm registry.
- **To run:** a Java 21 runtime and PostgreSQL 17.
- **Commands today:**
  - `./mvnw verify`: the build and every test;
  - `java -jar target/matriz-0.1.0-SNAPSHOT.jar`: start the application;
  - `docker compose up -d --wait`: start the database only.

## 3. External services and environment variables

The only external service is **PostgreSQL**. There is no email, queue or third-party API.

| Variable | Purpose | Secret | Default | Where |
|---|---|---|---|---|
| `MATRIZ_DB_URL` | JDBC connection | No | none; the application doesn't start without it | `application.yaml` |
| `MATRIZ_DB_USER` | Database user (also used by Compose) | No | none | `application.yaml`, `compose.yaml` |
| `MATRIZ_DB_PASSWORD` | Database password (also used by Compose) | **Yes** | none | `application.yaml`, `compose.yaml` |
| `MATRIZ_DB_NAME` | Database name, Compose only | No | required | `compose.yaml` |
| `MATRIZ_DEMO_SEED` | Adds the demo accounts and projects | No | `false` | `application.yaml` |
| `MATRIZ_DEMO_PASSWORD_ANA`, `_RUI`, `_SARA` | Demo account passwords. With the seed on, the application doesn't start without them (8 to 128 characters) | **Yes** | none | `application.yaml`, `DemoAccountsSeeder.java` |
| `MATRIZ_COOKIE_SECURE` | `Secure` flag of the session cookie | No | `true` | `application.yaml` |
| `SERVER_PORT` | HTTP port (Spring Boot convention) | No | `8080` | README |
| `VITE_API_BASE` | API base path in the browser, **build time only** | No | `/api` | `frontend/src/config.ts` |
| `MATRIZ_API_PORT` | API port for the Vite development server | No | `8080` | `frontend/vite.config.ts` |
| `E2E_PORT` | Port of the end-to-end test | No | `8095` | `frontend/playwright.config.ts` |

The application reads a `.env` file from its working directory (`spring.config.import` in `application.yaml`). The image must never contain one: `.dockerignore` excludes it, and the container gets its values from environment variables only.

## 4. Automated tests

`./mvnw verify` runs every test, locally and in CI:
- **213 back-end tests:** unit tests and integration tests against PostgreSQL in Testcontainers, including `ModularityTest`;
- **46 browser application tests** (Vitest);
- **1 end-to-end test** (Playwright, Chromium) against the built JAR.

Testcontainers needs Docker, so the tests can't run inside `docker build`. The image is therefore built **without tests**, and in CI only after the `build` job has passed.

## 5. Strategy

### 5.1 Containers

Two containers:

| Container | Runs | Image |
|---|---|---|
| `app` | Spring Boot with the browser application inside the JAR. It serves the static files and the API | Built from the project's `Dockerfile` |
| `postgres` | PostgreSQL 17.10 | The official `postgres:17.10`, as in `compose.yaml` today |

**Why the browser application has no container of its own** (discussed with the user, 2026-11-15):
- ADR-0001 and [architecture.md §5](architecture.md#5-technology-stack) define one deployable unit.
- The session cookie is `SameSite=Lax` (ADR-0002), so the browser application and the API must be served from the same site. With both in one JAR, no proxy or CORS is needed.
- The browser application and the back end are already separated in the code (`frontend/`, its own build and tests) and only connected through the API in `openapi.yaml`. A separate container would not reduce that coupling. It would add a proxy, an image, and a version match between two deployables.
- If a concrete need appears (a separate team, a CDN, a different release pace), the browser application can move to its own container later. That would need a new ADR.

### 5.2 Image (multi-stage)

- **Build stage:** `eclipse-temurin:21-jdk`. Copy the Maven wrapper, `pom.xml` and `frontend/package*.json` first, so the dependency layers stay cached, then the sources. Run `./mvnw package -DskipTests`.
- **Final stage:** `eclipse-temurin:21-jre` with only the JAR. It runs as a non-root user, exposes port 8080, and has no source code, build tools, Node, `.env` or secrets. Only the final stage is published, so the build layers are not part of the image.
- **Base images** pinned by digest.
- **Configuration** from environment variables only. No secret in `ARG` or `ENV`.

### 5.3 Docker Compose

- A new `app` service, built from the `Dockerfile`. It starts when `postgres` is healthy and connects to it by the service name, so it sets its own `MATRIZ_DB_URL` (the one in `.env` points to `localhost`). The other values come from `.env`.
- The service is in a Compose profile, `app`:
  - `docker compose up -d --wait` keeps starting the database only, as the README and AGENTS.md describe today;
  - `docker compose --profile app up -d --wait` starts both.

### 5.4 GitHub Actions

- **A new `image` job**, after `build`. It builds the image with Buildx, with the cache in `type=gha`, then a smoke test: start Compose with the `app` profile and check that `/actuator/health` answers `UP`.
- **Publication to the GitHub Container Registry (`ghcr.io`)**, approved by the user (2026-11-15):
  - only on pushes to `main` and on version tags, never on pull requests;
  - tagged with the commit SHA, and with the version on a version tag;
  - `packages: write` only in the job that publishes;
  - logged in with the workflow's `GITHUB_TOKEN`, no stored secret.

## 6. Files to create or change

| File | Action | Why |
|---|---|---|
| `Dockerfile` | Create | The multi-stage image |
| `.dockerignore` | Create | Keeps `.env`, `target/`, `node_modules/`, `.git/` and test results out of the build context |
| `compose.yaml` | Change | The `app` service, in the `app` profile |
| `.github/workflows/ci.yml` | Change | The `image` job, with the smoke test and the publication |
| `README.md`, `AGENTS.md` §10 | Change | The image and Compose commands |
| `docs/architecture.md` §5 | Change | "Packaging" mentions one container image, which doesn't exist yet. It will point to the `Dockerfile` |
| `docs/verification.md` | Change | The record of the image, the Compose run and the first `image` job |

## 7. Implementation steps

1. **Image:** `.dockerignore` and `Dockerfile`. Build locally, run it with the database, and check the health endpoint, the non-root user, that there is no `.env` in the image, and the image size.
2. **Compose:** the `app` service with its profile. Run `docker compose --profile app up`, check the health endpoint and a demo log-in, and check that the data survives a restart.
3. **CI:** the `image` job with the smoke test, without publishing.
4. **Publication:** the push to `ghcr.io` from `main` and version tags.
5. **Documentation and records:** README, AGENTS.md, architecture.md and verification.md.

Each step is checked before the next one starts.

## 8. Risks and open points

- **HTTPS (SEC08), PD13:** who terminates TLS (a proxy, a load balancer or the platform) and whether Spring needs `server.forward-headers-strategy`. The application sends no redirects and its `Location` headers are relative, so it may not be needed. To confirm once the platform is chosen.
- **Production database, PD13:** a managed PostgreSQL or a container, and its backups.
- **Production secrets, PD13:** the platform's secret store.
- **Demo seed:** with `MATRIZ_DEMO_SEED=true` the application needs the three demo passwords to start. To be decided per environment.
- **New tools,** approved by the user (2026-11-15): the `eclipse-temurin` base images, the `docker/setup-buildx-action`, `docker/build-push-action` and `docker/login-action` actions, and the GitHub Container Registry. Actions pinned by commit, as in the existing workflow.
- **Image architecture:** CI builds `linux/amd64`, while the developer's machine is arm64. A multi-platform image is optional and makes the build slower.
- **Health check in the image:** not yet confirmed whether `eclipse-temurin:21-jre` includes `curl` or `wget`. If not, the health check stays in Compose and on the platform.
- **Out of scope:** Kubernetes, monitoring, automated backups, automatic base image updates (Dependabot), and deployment to a platform (PD13).

## 9. Differences between the documents and the code

Found during the analysis and reported to the user:
- [architecture.md §5](architecture.md#5-technology-stack) says "Spring Data JPA for data access", but there are no `@Entity` classes, and data access uses `JdbcClient`. JPA only validates the schema (`ddl-auto: validate`). Not corrected.
- The same section said "Spring Boot Test with MockMvc", but the API tests send real HTTP requests and don't use MockMvc. Corrected in the document at the user's request (2026-11-15). The tests didn't change.
- "Packaging" mentions one container image, which doesn't exist yet. Step 1 resolves it.
