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
- **Implementation:** started. The application and its persistence are set up (increment I0 of the [implementation plan](docs/implementation-plan.md)): Java 21, Spring Boot 4, PostgreSQL with Flyway, and a React + TypeScript browser application built into the same JAR ([docs/architecture.md, section 5](docs/architecture.md#5-technology-stack)).
- **Next step:** increment I1, log-in and log-out with the demo accounts (US19, US02).

## Running the application

**Requirements:** Java 21 and Docker (running). Node isn't needed to build: Maven downloads its own copy for the browser application.

1. **Configuration.** Copy the example file and replace every value. `.env` is ignored by Git; never commit it.
   ```bash
   cp .env.example .env
   ```
2. **Database.** Start PostgreSQL in a container (it reads `.env`). Data is kept in a Docker volume between restarts.
   ```bash
   docker compose up -d --wait
   ```
3. **Build and test.** Builds the back end and the browser application into one JAR and runs every test. The tests start their own throw-away PostgreSQL with Testcontainers, so Docker must be running.
   ```bash
   ./mvnw verify
   ```
4. **Start.** Applies the database migrations and, if `MATRIZ_DEMO_SEED=true`, creates the demo accounts and projects that don't exist yet. Existing data is never changed or deleted.
   ```bash
   java -jar target/matriz-0.1.0-SNAPSHOT.jar
   ```
   Or, without building the JAR first: `./mvnw spring-boot:run`. Use another port with `SERVER_PORT=8081`.
5. **Check.** `http://localhost:8080/actuator/health` answers `{"status":"UP"}` when the database is reachable, and `http://localhost:8080/` shows the start page.

**Working on the browser application:** `cd frontend && npm install && npm run dev` starts Vite on `http://localhost:5173`, which forwards `/api` to the application on port 8080. `npm test` runs its tests.

**Demo accounts** (made-up people, PRI05): `ana@example.com`, `rui@example.com` and `sara@example.com`, with the passwords set in `.env`. In the project *Website*, Ana is the manager, Rui is a member and Sara is not a member; in *App*, Rui is the manager and Ana is a member. Log-in arrives in I1.

**Stop:** `Ctrl+C` stops the application; `docker compose down` stops PostgreSQL and keeps its data (`docker compose down -v` deletes it).
