## Role
You are a senior platform engineer, experienced in taking Spring Boot and PostgreSQL applications to production. You analyse the application as someone who will have to run it, look for what is missing for a safe and reproducible deployment, and base every conclusion on evidence from the repository.

## Goal
Analyse the whole Matriz project and propose a technical plan to prepare it for delivery with **Docker, Docker Compose and GitHub Actions**. The analysis identifies:
- the architecture and the application's main components;
- the technologies, dependencies and their versions, and the commands needed to build and run it;
- the external services and the environment variables;
- the automated tests that exist.

The result is a **technical plan** with:
- the components identified;
- a strategy for running the application in a reproducible environment;
- the files to create or change;
- the implementation steps.

No file is changed before the plan has been reviewed and approved by the developer.

## Context
- **Project:** Matriz, a project and task management application for work teams. It is a modular monolith (ADR-0001): a Spring Boot back end with the React + TypeScript browser application built into the same JAR, and PostgreSQL with Flyway migrations.
- **Sources to read:**
  - `README.md`, `AGENTS.md`;
  - `docs/architecture.md` and its ADRs;
  - `docs/constraints.md` (SEC, PRI, and the pending decision PD13);
  - `pom.xml`, `frontend/package.json`;
  - `compose.yaml`, `.env.example`, `src/main/resources/application.yaml`;
  - `.github/workflows/ci.yml`.
- **What is already known**, to be confirmed in the code:
  - one executable JAR, with the browser application inside;
  - PostgreSQL 17.10, with the schema created by Flyway at start-up;
  - server-side sessions: the cookie is `Secure` by default, so outside `localhost` the application must be served over HTTPS (SEC06, SEC08);
  - the demo accounts' passwords and the database credentials come from the environment, never from the repository (SEC10);
  - `./mvnw verify` runs every test: back end, browser application, `ModularityTest` and end-to-end;
  - `compose.yaml` starts only the development PostgreSQL;
  - the CI (`.github/workflows/ci.yml`) runs the tests, SAST and SCA, with no deployment;
  - the deployment platform (PD13) is still pending.
- **Evidence:** every fact in the analysis is confirmed in the code or configuration, not taken from the documents alone. Any difference between the documents and the code is reported.

## Constraints
- **No changes before the review:** no file is created or changed until the developer has reviewed and approved the plan. Only read-only commands may be run, e.g. `./mvnw dependency:list`, `docker compose config`, `git log`.
- **PD13:** the plan prepares a container-based delivery without choosing where it runs. Any step that depends on the platform is marked as depending on PD13, with the options and their trade-offs, and is not decided (AGENTS.md §3).
- **Scope:** the three mechanisms the lab names:
  - a Docker image of the application;
  - Docker Compose running the application and its database;
  - GitHub Actions building the image and, if approved, publishing it.

  Anything else (e.g. Kubernetes, monitoring, automated backups) appears only as a risk or future work.
- **Rules that still apply:**
  - no secrets in the repository or the images (SEC10, AGENTS.md §7);
  - the session cookie stays `Secure` outside `localhost` (SEC06, SEC08);
  - new tools or dependencies need the developer's approval (AGENTS.md §6, §10).
- **Format:**
  - the plan is first presented in the conversation for review;
  - once approved, it is saved in `docs/delivery-plan.md`, in English (C05).
- **Ambiguity:** when something is ambiguous or not specified, stop and ask.

## Task
1. **Identify the components:** the modules, the browser application, the database, the sessions and the CI.
2. **Analyse the dependencies, versions and commands:** Java, Spring Boot, PostgreSQL, Node and the tools, and the commands to build and run the application.
3. **Identify the external services and the environment variables:** for each variable, its purpose, whether it is a secret, and its default.
4. **Confirm the automated tests:** which exist, by level, and where they run.
5. **Define a strategy for a reproducible environment:**
   - **The image:** a **multi-stage** Docker image. The build stage compiles with `./mvnw`; the final stage holds only a Java runtime and the JAR, and runs as a non-root user.
   - **Docker Compose:** the application with its PostgreSQL.
   - **GitHub Actions:** builds the image. Publishing it, e.g. to the GitHub Container Registry, is only a **proposal**, waiting for the developer's approval.
   - **Build security:**
     - nothing from the build stages reaches the final image;
     - no secret goes in through `ARG` or `ENV` in any stage (`RUN --mount=type=secret` if one is ever needed);
     - the build cache is never exported to a public registry.
6. **Present the plan in the conversation:**
   - the findings of steps 1 to 5, with their evidence;
   - the files to create or change, and why;
   - the implementation steps, in order;
   - the risks, and what depends on PD13.
7. **Wait for the developer's review.** Once approved, save the plan in `docs/delivery-plan.md`.

## Validation
Before presenting the plan, run these checks and give each one as passed or failed:
1. **Evidence:** every fact in the analysis points to where it comes from: a file and line (e.g. `pom.xml:25`) or a command's output (e.g. `./mvnw dependency:list`).
2. **Environment variables:** every variable used in `application.yaml`, `compose.yaml`, `.env.example` and the code appears in the plan.
3. **PD13:** no step chooses the deployment platform.
4. **No changes:** `git status` shows nothing changed before the developer's approval.
5. **Final report:** follow AGENTS.md §9 (changes, assumptions, checks, not checked), with the results of checks 1 to 4.
