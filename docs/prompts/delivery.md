## Role

You are a senior technical reviewer experienced in Java, React and application security. You review the project as the course's assessor would, looking for gaps between what is documented and what is built.

## Goal

Review the Matriz MVP and prepare it for delivery, so that it meets every completion criterion:
- the application starts by following the README;
- the MVP lets users manage projects, tasks, assignees and comments through the interface;
- authentication and authorization also hold for direct requests to the API;
- data persists after reloading the interface and restarting the back end;
- the interface handles loading, empty, success and error states;
- the contract matches the implementation, and the checks have recorded results;
- the code has been reviewed, and the delivered version is identified in Git.

The delivered version is identified by an annotated Git tag **`v1.0.0`** (major.minor.patch) on the final commit.

**Deliverables, in the Matriz repository:**
- the full front-end and back-end code, with tests and dependency files;
- the schema (migrations), the demo data and an example configuration;
- README.md, AGENTS.md and the OpenAPI contract, up to date;
- the earlier documentation consistent with the solution, together with `docs/implementation-plan.md` and `docs/verification.md`.

## Context

- **Project:** Matriz, a simplified Jira-like project and task management application. The MVP is complete:
  - increments I0–I6, the 23 user stories;
  - a modular monolith in Java 21 and Spring Boot 4 with PostgreSQL and Flyway;
  - a React + TypeScript browser application served from the same JAR;
  - 174 back-end and 46 browser application tests.
- **Already done:**
  - the verification of exercise 8, in `docs/verification.md`: 63 checks, all Pass;
  - one or more commits per exercise, up to `d9da830`.
- **Environment:**
  - the project is in `~/Matriz`;
  - PostgreSQL runs in `docker compose`;
  - the application runs on port 8081, because port 8080 is in use on this machine;
  - the local `.env` sets `MATRIZ_COOKIE_SECURE=false` for Safari on `http://localhost`.
- **Documents to review:**
  - `README.md` and `AGENTS.md`;
  - `openapi.yaml` and `docs/api-contract.md` (assumptions AS19–AS23 may not be in `openapi.yaml` yet);
  - `docs/implementation-plan.md` and `docs/verification.md`;
  - `docs/requirements.md`, `docs/user-stories.md`, `docs/constraints.md`;
  - `docs/architecture.md`, `docs/architecture/`, `docs/database.md` and `docs/ui/README.md`.

## Constraints

- **Show first, then fix:** problems found in the review (permissions, validation, error handling, changes out of scope) are reported with their evidence, and fixed only after the user approves, each with a regression test.
- **Refactoring:** goes in a separate commit. Behaviour must stay the same, shown by the tests before and after, and the decision is recorded, even a decision not to refactor.
- **The documentation describes what is built.** When the documentation and the code disagree on the stories or the contract, the user decides which one changes.
- **No new features and no new dependencies.**
- **Historical records are not edited:** `docs/requirements-v1.md`, `docs/prompts/` and the closed reviews.
- **No secrets in any document,** and `.env` stays out of Git.
- **Commits:** small and focused, with clear messages and the prefix "Lab 03, exercise 9:".
- **Pushing and tagging** need the user's approval.

## Task

1. **Review the diff** of all the code since I0, with AI support:
   - confirm the acceptance criteria are met;
   - inspect permissions, validation, error handling and changes out of scope;
   - present the findings with their evidence, and wait for approval.
2. **Refactoring:** pick one opportunity to improve the code and show it before and after. If it is worth it, apply it in its own commit, with the tests passing before and after. Record the decision in `docs/implementation-plan.md`.
3. **Contract:**
   - bring `openapi.yaml` in line with the implementation (e.g. assumptions AS19–AS23 and the assignee order);
   - review `docs/api-contract.md` and the other affected documents.
4. **AGENTS.md:** the commands to run and verify the project.
5. **README.md:**
   - requirements and installation;
   - configuration and `.env`, including `MATRIZ_COOKIE_SECURE`;
   - how the schema is created (Flyway migrations);
   - the seed and the made-up demo accounts;
   - starting the application;
   - running the tests.
6. **docs/verification.md:** add three examples of AI use, each with the aim of the request, the developer's decision and how it was checked:
   - the Safari log-in diagnosis (the `Secure` cookie);
   - assigning people while creating a task, an option the user rejected;
   - the PD05 concurrency test, checked to fail without the lock.
7. **Clean copy:** `git clone` into a new folder and follow the README word for word, from `cp .env.example .env` to opening the application and running the tests. Record the result.
8. **Commits and tag:** small commits; then ask for approval to push and to tag `v1.0.0`; then report as in AGENTS.md §9.

## Validation

1. **The application starts from the README:** the result of the clean copy in Task step 7.
2. **The MVP works through the interface, authorization holds in the API, and data persists:** the verification of exercise 8 (`docs/verification.md`); the affected parts are run again if anything changes.
3. **Interface states:** the browser application tests.
4. **The contract matches the implementation:** the lint of `openapi.yaml`, and a comparison of the contract's operations with the ones the code exposes.
5. **Reviewed and identified:** the recorded review, the tag `v1.0.0`, and a clean `git status`.
6. **Full build:** `./mvnw verify` passes.
7. **The needed files are in Git:**
   - the dependency files (`pom.xml`, `package-lock.json`), the migrations, `.env.example` and the fonts are versioned;
   - `.env`, `target/` and `node_modules/` are not.
