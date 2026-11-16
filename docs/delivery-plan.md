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
  - only when a GitHub release is published, never on pushes, pull requests or manual runs. Changed from "pushes to `main` and version tags" when it was implemented (Challenge C, 2026-11-15), so that only a release publishes;
  - the image published is the one the `image` job built, scanned and tested, not a new build;
  - tagged with the version and the commit SHA, and with `latest` only for a release that is not a pre-release;
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

1. **Image:** `.dockerignore` and `Dockerfile`. Build locally, run it with the database, and check the health endpoint, the non-root user, that there is no `.env` in the image, and the image size. **Done** (verification.md §14).
2. **Compose:** the `app` service with its profile. Run `docker compose --profile app up`, check the health endpoint and a demo log-in, and check that the data survives a restart. **Done** (verification.md §15).
3. **CI:** the `image` job with the smoke test, without publishing. **Done**, with an image scan added (verification.md §16).
4. **Publication:** the push to `ghcr.io` when a release is published. **Done**, shown with the pre-release `v1.1.0-rc.1` (verification.md §17).
5. **Documentation and records:** README, AGENTS.md, architecture.md and verification.md. **Done**, released as `v1.1.0` (section 10).

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
- [architecture.md §5](architecture.md#5-technology-stack) said "Spring Data JPA for data access", but there were no `@Entity` classes, and data access uses `JdbcClient`. With no entities, `ddl-auto: validate` checked nothing. Resolved in `v1.1.1` (2026-11-16): JPA was removed and the document corrected.
- The same section said "Spring Boot Test with MockMvc", but the API tests send real HTTP requests and don't use MockMvc. Corrected in the document at the user's request (2026-11-15). The tests didn't change.
- "Packaging" mentioned one container image, which didn't exist yet. Resolved by step 1, and the line now describes the image (2026-11-16).

## 10. Delivery

- **Version:** Git tag `v1.1.0` on the branch `main`, released on GitHub as [v1.1.0](https://github.com/vitorhsbarros/matriz/releases/tag/v1.1.0). The tag marks the commit that adds this section. It is a minor release over `v1.0.1`: the application's behaviour is the same, plus the delivery below and two public health checks. A test pre-release, [v1.1.0-rc.1](https://github.com/vitorhsbarros/matriz/releases/tag/v1.1.0-rc.1), was published first to check the publication, and is kept.
- **Release notes and changelog:** [release-notes/v1.1.0.md](release-notes/v1.1.0.md), with the Semantic Versioning reasoning, and [CHANGELOG.md](../CHANGELOG.md).
- **Image:** published by CI from the release as `ghcr.io/vitorhsbarros/matriz:1.1.0`, `:sha-<commit>` and `:latest` ([package](https://github.com/users/vitorhsbarros/packages/container/package/matriz)). The check of the published image is recorded in [verification.md](verification.md), after the tag.

**What it contains:**

| Part | What | Evidence |
|---|---|---|
| Container image | Multi-stage `Dockerfile` and allow-list `.dockerignore`. A Java 21 runtime and the JAR, run as a non-root user on a read-only JAR, with Ubuntu security updates | [verification.md §14](verification.md#14-application-container-image), [§16](verification.md#16-continuous-integration-of-the-image) |
| Docker Compose | `compose.yaml`: the `app` service in the `app` profile, started once PostgreSQL is healthy. Ports on `127.0.0.1`, no capabilities, read-only file system. `.env.example` gains `MATRIZ_APP_PORT` | [§15](verification.md#15-running-the-whole-application-with-docker-compose) |
| CI | The `image` job, after the tests, SAST and SCA: build, image scan (OSV-Scanner, failing on fixable vulnerabilities), smoke test with Compose (`scripts/image-smoke-test.sh`). Shown failing on a broken test (pull request #1, closed) | [§16](verification.md#16-continuous-integration-of-the-image) |
| Publication | The `publish` job, on releases only, with `packages: write` only there and the `GITHUB_TOKEN`. It pushes the tested image, not a new build | [§17](verification.md#17-publishing-the-image-to-the-github-container-registry) |
| Observability | ECS JSON logs in the container, plain text outside it. Public `/actuator/health/liveness` and `/actuator/health/readiness`, readiness with the database. The image's health check uses readiness | [§18](verification.md#18-observability-structured-logs-liveness-and-readiness) |
| Documentation | README ("Technologies", "Running with Docker", CI, status), AGENTS.md §10, architecture.md §5 with the delivery diagram, `openapi.yaml` checked against the code (30 operations), `CHANGELOG.md`, the release notes, and this plan | — |

**Deliverables, checked on 2026-11-16:** `Dockerfile`, `.dockerignore`, `compose.yaml`, `.env.example`, `.github/workflows/ci.yml` and `scripts/image-smoke-test.sh` are in the repository. The evidence is in verification.md §14 to §18, and the prompts used are in `docs/prompts/` (`delivery-analysis.md`, `containerisation.md`, `compose.md`, `ci-image.md`, `image-release.md`, `observability.md`, `delivery-v1.1.0.md`, `technical-documentation.md`). `CHANGELOG.md` and `docs/release-notes/v1.1.0.md` are at their paths.

**Not run:**
- The deployment to a hosting platform: PD13 is still pending, so the image is published but runs nowhere.
- The image on arm64: it is built for `linux/amd64` only. On the developer's arm64 Mac it was run under emulation.
- A final release's `latest` tag is first checked with this release, after the tag (verification.md).

**Open points:**
- **PD13**, the deployment platform, with what depends on it (section 8): TLS and forwarded headers, the production database and its backups, the secret store.
- **Readiness while the database is down** answers after 30 s, Hikari's connection time-out (verification.md §18). It is still right for the image's health check. A shorter time-out was left for the developer to decide.
- **Supply chain:** the image has no signature or provenance attestation, and the scan doesn't see the Java runtime (verification.md §16, §17).
- **Version numbers:** `pom.xml` still says `0.1.0-SNAPSHOT`, which the JAR's name and the logs' `service.version` show, while the release is `v1.1.0`. It was the same for `v1.0.1`. Not changed.
- **Documents and code:** architecture.md §5 says "Spring Data JPA for data access", while the code uses `JdbcClient` (section 9). Not changed.

### Follow-up: v1.1.1

On 2026-11-16 the developer asked to solve the open points above. Released as `v1.1.1`, a patch ([release-notes/v1.1.1.md](release-notes/v1.1.1.md), evidence in [verification.md §20](verification.md#20-release-v111-the-open-points-of-v110)):

| Open point | Outcome |
|---|---|
| Readiness while the database is down | **Solved:** the connection time-out is 5 s, so readiness answers `DOWN` in about 5 s instead of 30 s |
| Supply chain: no signature | **Solved for provenance:** each published image has a signed provenance attestation (GitHub artifact attestations, Sigstore), checked with `gh attestation verify`. The scan still doesn't see the Java runtime |
| Version numbers | **Solved:** `pom.xml` follows the releases (`1.1.1`), and the JAR is `target/matriz.jar` whatever the version |
| "Spring Data JPA" in architecture.md | **Solved:** JPA removed (it was never used), data access through Spring JDBC, document corrected |
| PD13 | **Still pending,** the developer's decision. The options are compared in [deployment-options.md](deployment-options.md) |
| amd64 only | Unchanged: multi-platform images are out of scope |

