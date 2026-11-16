# Matriz

Matriz is a project and task management application for work teams, similar to Jira but much simpler. Several people can create projects, record the work as tasks with a priority, follow each task on a board (To do, In progress, Done), discuss it in comments and make clear who is responsible for it: one or more people or teams.

## Problem

The client needs an application where its teams can organise projects, manage tasks and follow the progress of the work. The project assumes these are small teams for whom tools like Jira bring sprints, reports, workflows and configuration they don't need (ASM12). Matriz keeps only what is needed to organise the work.

## Goals

1. Let a team organise its work in projects, each with its own tasks and members.
2. Make clear who is responsible for each task: one or more people or teams.
3. Show at a glance where every task stands.
4. Stay simple enough to use without training: no sprints, reports, integrations or configurable workflows.

## MVP features

| Area | What a user can do | User stories |
|---|---|---|
| **Accounts** | Log in with a demo account created by the seed, see who is logged in, and log out. | US19, US02 |
| **Home dashboard** | See their projects with the number of tasks in each status, per project and in total, and search projects by name. | US04, US23 |
| **Projects** | Create a project, open it, and (as manager) edit or delete it and add and remove members. | US03, US09, US14, US15 |
| **Teams** | Create a team and (as manager) rename or delete it and add and remove its members. | US11, US12, US17, US18 |
| **Tasks** | Create, view, edit and delete tasks with a priority, move them between To do, In progress and Done on the project's board, filter the board by status and priority, and assign several members and teams to them. | US05–US08, US10, US13, US16, US20, US24 |
| **Comments** | Add and read a task's comments, and edit or delete their own. | US21, US22 |

Public registration and password recovery are optional extensions, outside the MVP.

The full MVP scope, and what is left for later, is in [docs/requirements.md](docs/requirements.md).

## Technologies

The versions are the ones the build uses, from `pom.xml` (and Spring Boot's managed versions), `frontend/package.json`, `compose.yaml` and the `Dockerfile`. Why each part was chosen is in [docs/architecture.md, section 5](docs/architecture.md#5-technology-stack).

| Part | Technology |
|---|---|
| Language and runtime | Java 21 (Eclipse Temurin 21.0.13 in the image) |
| Application | Spring Boot 4.1.2: Spring Web MVC for the JSON API, Spring Security 7.1 (session cookie and CSRF), Spring Session JDBC, Spring Boot Actuator (health checks), Bean Validation |
| Module boundaries | Spring Modulith 2.1.2, checked by `ModularityTest` |
| Database | PostgreSQL 17.10, driver 42.7.14, schema by Flyway 12.4.0 migrations (one folder per module), data access with Spring's `JdbcClient` |
| Browser application | React 19.3.1 and TypeScript 5.9.3, built with Vite 8.3.5 into the JAR |
| Build | Maven 3.9.11 (wrapper), `frontend-maven-plugin` 2.0.2 with Node v24.22.3 |
| Tests | JUnit 6.0.3, Spring Boot Test and Testcontainers 2.0.5 (back end), Vitest 4.1.12 and Testing Library 16.3.3 (browser application), Playwright 1.64.1 with Chromium (end to end) |
| Containers | Docker with a multi-stage `Dockerfile`, Docker Compose |
| CI and delivery | GitHub Actions: Semgrep, CodeQL, OSV-Scanner (with a CycloneDX SBOM), Docker Buildx, and the GitHub Container Registry |

## Documentation

### Specification Pack

| Document | Contents |
|---|---|
| [docs/requirements.md](docs/requirements.md) | Vision and goals, user types, functional and non-functional requirements, MVP scope, and what is out of the initial scope. |
| [docs/user-stories.md](docs/user-stories.md) | The 23 MVP user stories, with priorities, acceptance criteria and exceptional cases, and the optional extensions. |
| [docs/constraints.md](docs/constraints.md) | Known constraints, assumptions, security and privacy rules, pending technical decisions and scope limitations. |
| [AGENTS.md](AGENTS.md) | Instructions for AI coding agents working on this repository. |

### Architecture Pack

| Document | Contents |
|---|---|
| [docs/architecture.md](docs/architecture.md) | The chosen architecture and its justification, with links to the alternatives, the domain model and the decision records (ADRs). |
| [docs/database.md](docs/database.md) | Logical data model of the MVP and ER diagram. |
| [openapi.yaml](openapi.yaml) | API specification of the MVP (OpenAPI 3.1). The reasoning behind it is in [docs/api-contract.md](docs/api-contract.md). |
| [docs/ui/](docs/ui/) | Low-fidelity wireframe of the main screens, with a link to the interactive prototype. |

### Process records

| Folder | Contents |
|---|---|
| [docs/test-plan.md](docs/test-plan.md) | Test plan for tasks and their permissions, derived from the requirements: scenarios, expected results and the tests that check each one. |
| [docs/quality-security-review.md](docs/quality-security-review.md) | Code and security review: findings with their evidence, the decisions taken, the fixes and their regression tests, and what was left unverified. |
| [CHANGELOG.md](CHANGELOG.md) | Every release, newest first, with what was added, changed and fixed. |
| [docs/release-notes/](docs/release-notes/v1.1.0.md) | Release notes of the current version, with its version number reasoning and known issues. |
| [docs/delivery-plan.md](docs/delivery-plan.md) | Delivery plan: the analysis behind the container image, Docker Compose and the CI workflow, the steps, and the "Delivery" section of the current version. |
| [docs/verification.md](docs/verification.md) | Verification of the MVP: for each scenario, the expected result, the observed result and the evidence (tests, screenshots in [docs/verification/](docs/verification/), database checks), and how the data survives restarts. |
| [docs/reviews/](docs/reviews/) | Critical reviews of the documentation, with the decisions taken on each finding. |
| [docs/prompts/](docs/prompts/) | The prompts used to produce the documentation with AI assistance. |
| [docs/requirements-v1.md](docs/requirements-v1.md) | First version of the requirements, kept for history. |

## Current status

- **Specification and architecture:** done.
  - The requirements, user stories, constraints, architecture, data model, API and wireframe are written.
  - The documentation was produced with AI assistance and reviewed critically, with every correction decided by a person:
    - the [specification review](docs/reviews/specification-review.md) covers the Specification Pack;
    - [review-01](docs/reviews/review-01.md) covers the Architecture Pack.
  - A [prompt injection test](docs/reviews/prompt-injection-challenge.md) checked that agents treat external documents as data.
  - The MVP was then [reconciled with a new scope](docs/reviews/scope-reconciliation.md) before implementation.
- **Implementation:** started. Java 21, Spring Boot 4, PostgreSQL with Flyway, and a React + TypeScript browser application built into the same JAR ([docs/architecture.md, section 5](docs/architecture.md#5-technology-stack)). Done so far, from the [implementation plan](docs/implementation-plan.md):
  - I0: the application and its persistence are set up;
  - I1: log-in, the current user and log-out with the demo accounts (US19, US02), and the project access check that the next increments use;
  - I2: project management, the first complete path from the screen to the database: the home dashboard with task counts and search, and creating, opening, editing and deleting projects (US03, US04, US23, US14, US15);
  - I3: the work of each project: the task board, and creating, moving, editing, viewing, deleting and filtering tasks (US05, US06, US07, US08, US20, US16, US24);
  - I4 to I6: project members, teams, assigning people and teams to tasks, and comments (US09–US13, US17, US18, US21, US22).

  Every MVP story can be used through the browser application, which has its own visual system ([docs/ui/README.md](docs/ui/README.md#visual-system-of-the-browser-application)).
- **Verification and delivery:** every MVP scenario was verified and recorded in [docs/verification.md](docs/verification.md), and the code was reviewed for the delivery ([docs/implementation-plan.md](docs/implementation-plan.md#review-for-the-delivery--done-on-2026-11-12)). The MVP was delivered as the Git tag `v1.0.0`.
- **Tests and quality (Lab 04):**
  - tests derived from the requirements ([docs/test-plan.md](docs/test-plan.md)): unit, integration and end-to-end, all run by `./mvnw verify`;
  - a code and security review ([docs/quality-security-review.md](docs/quality-security-review.md)), whose six findings were fixed.

  That work was released as the Git tag `v1.0.1`: a patch with fixes and tests, and no new features ([docs/quality-security-review.md, "Delivery"](docs/quality-security-review.md#delivery)).
- **Delivery (Lab 05):** planned in [docs/delivery-plan.md](docs/delivery-plan.md):
  - a container image, run as a non-root user;
  - Docker Compose for the application and PostgreSQL together;
  - a CI job that builds, scans and tests the image once every check has passed;
  - publication of that image to `ghcr.io` on each release;
  - JSON logs in the container, and public liveness and readiness checks.

  The current version is the Git tag **`v1.1.0`** on the branch `main`, published as the image `ghcr.io/vitorhsbarros/matriz:1.1.0`. Its [release notes](docs/release-notes/v1.1.0.md) and the [CHANGELOG](CHANGELOG.md) say what changed; what wasn't run and what is still open is listed in [docs/delivery-plan.md, "Delivery"](docs/delivery-plan.md#10-delivery).
- **Not done yet:** the deployment to a hosting platform (PD13, pending).

## Running the application

### Requirements

| What | Version | Why |
|---|---|---|
| Java (JDK) | 21 | Builds and runs the application. |
| Docker, running | Docker Desktop or Docker Engine with Compose | Runs PostgreSQL 17 for the application, and a throw-away PostgreSQL for the tests (Testcontainers). |
| Git | any recent | To get the code. |
| Free ports | 5432 (PostgreSQL) and 8080 (the application; another one can be chosen) | |

Node and npm aren't needed: Maven downloads its own copy (Node 24) to build and test the browser application. Maven isn't needed either: the project includes the Maven wrapper (`./mvnw`).

### 1. Get the code

```bash
git clone https://github.com/vitorhsbarros/matriz.git
cd matriz
```

### 2. Configure

Copy the example configuration. `.env` is ignored by Git; never commit it.

```bash
cp .env.example .env
```

Then replace the example values with your own. Every value in `.env.example` is made up, and works as it is for a local try-out.

| Variable | What it sets |
|---|---|
| `MATRIZ_DB_NAME`, `MATRIZ_DB_USER`, `MATRIZ_DB_PASSWORD` | The database, user and password that `compose.yaml` creates, and that the application uses. |
| `MATRIZ_DB_URL` | Where the application finds the database: `jdbc:postgresql://localhost:5432/matriz`. |
| `MATRIZ_DEMO_SEED` | `true` to create the demo accounts and projects at start-up (see below). |
| `MATRIZ_DEMO_PASSWORD_ANA`, `…_RUI`, `…_SARA` | The demo accounts' passwords, 8 to 128 characters. They are stored only as BCrypt hashes. |
| `MATRIZ_COOKIE_SECURE` | `false` only for `http://localhost`, so Safari keeps the session cookie; `true` (the default if unset) anywhere else, which must use HTTPS. |
| `SERVER_PORT` (optional) | The application's port, 8080 by default. |

### 3. Start the database

```bash
docker compose up -d --wait
```

This starts PostgreSQL 17.10 in a container, with the database, user and password from `.env`. The data is kept in a Docker volume between restarts.

### 4. Build and run the tests

```bash
./mvnw verify
```

This builds the back end and the browser application into one JAR (`target/matriz-0.1.0-SNAPSHOT.jar`) and runs every test:
- the back-end tests, which start their own throw-away PostgreSQL with Testcontainers;
- the browser application tests (Vitest);
- `ModularityTest`, which checks the module boundaries;
- the end-to-end test (Playwright): a real Chromium logs in, opens a project, creates a task and finds it on the board. It runs against the JAR just built and a throw-away PostgreSQL in Docker, on port 8095 (`E2E_PORT` changes it), so port 8095 must be free. It never touches your own database or `.env`.

The first run downloads the dependencies, and Chromium for the end-to-end test (about 100 MB), and takes a few minutes. Later runs take about two and a half. To run one back-end test class only: `./mvnw test -Dtest=TasksApiTest -Dskip.npm -Dskip.installnodenpm`. To skip only the end-to-end test: `./mvnw verify -Dskip.e2e`.

On GitHub, the CI workflow ([.github/workflows/ci.yml](.github/workflows/ci.yml)) runs on every push and pull request to `main`:
1. **Checks:** the same `./mvnw verify`, the static analysis of the code (Semgrep, CodeQL), and the dependency scan (OSV-Scanner).
2. **Image:** once all of those pass, it builds the container image, scans it for known vulnerabilities and tests it with Docker Compose (see [Running with Docker](#running-with-docker)).
3. **Publication:** when a release is published, it also pushes that tested image to `ghcr.io`.

### 5. Start the application

```bash
java -jar target/matriz-0.1.0-SNAPSHOT.jar
```

Or, without building the JAR first: `./mvnw spring-boot:run`. To use another port: `SERVER_PORT=8081 java -jar target/matriz-0.1.0-SNAPSHOT.jar`.

At start-up the application:
- **creates or updates the schema** with the Flyway migrations in `src/main/resources/db/migration/<module>`. Nothing has to be created by hand, and existing data is never changed by a migration;
- **adds the demo data,** when `MATRIZ_DEMO_SEED=true`: the three accounts and two projects below, only those that don't exist yet. Existing data is never changed or deleted, so restarting is safe.

### 6. Open it

- `http://localhost:8080/actuator/health` answers `{"status":"UP"…}` when the database is reachable.
- `http://localhost:8080/` shows the log-in screen. Chrome, Firefox and Safari work.

**Demo accounts** (made-up people, PRI05), with the passwords set in `.env`:

| Account | Name | In *Website* | In *App* |
|---|---|---|---|
| `ana@example.com` | Ana Silva | manager | member |
| `rui@example.com` | Rui Costa | member | manager |
| `sara@example.com` | Sara Lopes | not a member | not a member |

Log in as different people to see the access rules: Sara gets "Project not found." for *Website*, and Rui can work on its tasks but can't edit or delete it.

### Stop

`Ctrl+C` stops the application. `docker compose down` stops PostgreSQL and keeps its data; `docker compose down -v` deletes it.

### Running with Docker

The application can also run in a container, next to PostgreSQL, so only Docker is needed. It reads the same `.env` (step 2), with one difference: the container reaches the database through Compose, so it ignores `MATRIZ_DB_URL`.

**Build the image** (`matriz:local`) from the `Dockerfile`. Maven builds the JAR inside the build, without running the tests, so run `./mvnw verify` for those. The first build takes a few minutes.

```bash
docker compose --profile app build
```

**Start PostgreSQL and the application.** The command returns once both are healthy: the application is healthy when it is ready to serve requests, which needs the database. Without `--profile app`, only PostgreSQL starts, as in step 3.

```bash
docker compose --profile app up -d --wait
```

The application is then on `http://localhost:8080/`, and PostgreSQL on port 5432, both reachable from this machine only (`127.0.0.1`). To use another port for the application, set `MATRIZ_APP_PORT`, in `.env` or on the command line: `MATRIZ_APP_PORT=8082 docker compose --profile app up -d --wait`.

**Health checks.** All three can be read without logging in. They show only a status, and `/actuator/health` also the names of the two others:

| URL | Answers `UP` when | When the database can't be reached |
|---|---|---|
| `/actuator/health` | everything is up | `DOWN` (503) |
| `/actuator/health/liveness` | the application is running | still `UP`, so a platform doesn't restart it for that |
| `/actuator/health/readiness` | it can serve requests | `DOWN` (503) |

**Logs.** In the container, each line is a JSON object in the Elastic Common Schema (ECS), for a log collector. Outside the container the logs stay plain text.

```bash
docker compose logs app                                    # the JSON lines
docker compose logs --no-log-prefix app | jq -r .message   # only the messages
```

**Check it.** `scripts/image-smoke-test.sh` checks the running containers, the same way CI does:
- **the image:** a non-root user, no `.env` or sources, a read-only file system, ports on `127.0.0.1`;
- **the application:** the health checks, the JSON logs, log-in, CSRF, and creating and reading a task;
- **persistence:** the task is still there after the containers are recreated.

It needs `curl`, `jq` and `MATRIZ_DEMO_SEED=true`, and uses `MATRIZ_APP_PORT` if it is set. It deletes the task it creates.

```bash
scripts/image-smoke-test.sh
```

**Stop.** `docker compose --profile app down` stops both containers and keeps the data; `-v` would delete it.

**The published image.** Each release is published to the GitHub Container Registry by CI, as `ghcr.io/vitorhsbarros/matriz:<version>` (and `latest` for the newest final release). It is public and built for `linux/amd64`; on an arm64 machine Docker runs it under emulation, which is slower. To run it instead of a local build:

```bash
docker pull ghcr.io/vitorhsbarros/matriz:1.1.0
docker tag ghcr.io/vitorhsbarros/matriz:1.1.0 matriz:local
docker compose --profile app up -d --wait --no-build
```

### Working on the browser application

`cd frontend && npm ci && npm run dev` starts Vite on `http://localhost:5173`, which forwards `/api` to the application on port 8080, or on `MATRIZ_API_PORT` (e.g. `MATRIZ_API_PORT=8081 npm run dev`). `npm test` runs its tests. `npm run e2e` runs the end-to-end test on its own, once the JAR has been built (`./mvnw package -DskipTests`); when it fails, it keeps a screenshot and a trace in `frontend/test-results/` (`npx playwright show-trace <trace.zip>`). This needs Node 24 installed.

**What can be done now** (every MVP story):
- **Log in and out** with a demo account. The header shows who is logged in.
- **Home dashboard** (*My projects*): your projects, sorted by name, with your role and the number of tasks in each status, and the totals in *All my projects*. **Search projects** narrows the list; the totals stay the same.
- **New project:** you become its manager.
- **Open a project** from the list to see its **board**: the columns *To do*, *In progress* and *Done*, each with its number of tasks, newest first. Its address (`#/projects/<id>`) survives a reload.
- **Tasks** (any member, on any task of the project):
  - **New task** with a title (3–150 characters), an optional description (up to 2000), an optional due date (not in the past) and a priority (*Medium* by default);
  - change the status with the ← → buttons, by dragging the card to another column, or in the task's details;
  - filter the board by status and priority, and **Clear filters**;
  - open a task for its details (`#/projects/<id>/tasks/<id>`), **Edit** it, or **Delete task** after confirming (also on each card).
  - The board and its counts update at once, without reloading; the home dashboard shows the new counts when it is opened again.
  - on the task's details, **assign** people and teams (members of the project, and teams whose members are all in it) and remove them;
  - **comment** on a task; only the author edits or deletes a comment, which then shows "(edited)".
- **Members** tab of a project: every member sees who is in it; the manager adds people by email and removes them.
- **Teams** (in the header): every user sees every team and can create one; the team's manager renames it, deletes it, and adds and removes members.
- **As the manager:** **Edit** the name and description, and **Delete project** after confirming. Deleting also deletes the project's tasks and comments.
- Members see the project but not Edit or Delete; anyone else gets "Project not found.".
- Projects are stored in PostgreSQL, so they are still there after restarting the application.

**Sessions** ([ADR-0002](docs/architecture/adr/ADR-0002-session-cookie-authentication.md); details in [docs/api-contract.md, section 4](docs/api-contract.md#4-authentication)):
- Log-in starts a session on the server, stored in PostgreSQL, and sets the `MATRIZ_SESSION` cookie (`HttpOnly`, `Secure`, `SameSite=Lax`). Every log-in gets a new session identifier and a new CSRF token.
- The browser application sends the CSRF token in the `X-CSRF-Token` header on every request that changes data. It keeps the token in memory only.
- A session ends after **30 minutes without requests** (PD02), or at once with **Log out**. The application then shows the log-in screen with "Please log in to continue.".
- The cookie is `Secure`, so outside your own machine the application must be served over HTTPS (SEC08). Safari doesn't keep a `Secure` cookie on `http://localhost` (log-in is accepted but doesn't stick), so a local `.env` sets `MATRIZ_COOKIE_SECURE=false`, as in `.env.example`. Leave it `true`, or unset, anywhere else.

