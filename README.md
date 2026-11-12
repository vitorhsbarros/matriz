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
- **Implementation:** started. Java 21, Spring Boot 4, PostgreSQL with Flyway, and a React + TypeScript browser application built into the same JAR ([docs/architecture.md, section 5](docs/architecture.md#5-technology-stack)). Done so far, from the [implementation plan](docs/implementation-plan.md):
  - I0: the application and its persistence are set up;
  - I1: log-in, the current user and log-out with the demo accounts (US19, US02), and the project access check that the next increments use;
  - I2: project management, the first complete path from the screen to the database: the home dashboard with task counts and search, and creating, opening, editing and deleting projects (US03, US04, US23, US14, US15);
  - I3: the work of each project: the task board, and creating, moving, editing, viewing, deleting and filtering tasks (US05, US06, US07, US08, US20, US16, US24);
  - I4 to I6: project members, teams, assigning people and teams to tasks, and comments (US09–US13, US17, US18, US21, US22).

  Every MVP story can be used through the browser application, which has its own visual system ([docs/ui/README.md](docs/ui/README.md#visual-system-of-the-browser-application)).
- **Next step:** to be decided with the user, for example the deployment (PD13).

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
5. **Check.** `http://localhost:8080/actuator/health` answers `{"status":"UP"}` when the database is reachable, and `http://localhost:8080/` shows the log-in screen.

**Working on the browser application:** `cd frontend && npm install && npm run dev` starts Vite on `http://localhost:5173`, which forwards `/api` to the application on port 8080 (or on `MATRIZ_API_PORT`, e.g. `MATRIZ_API_PORT=8081 npm run dev`). `npm test` runs its tests.

**Demo accounts** (made-up people, PRI05): `ana@example.com`, `rui@example.com` and `sara@example.com`, with the passwords set in `.env`. In the project *Website*, Ana is the manager, Rui is a member and Sara is not a member; in *App*, Rui is the manager and Ana is a member. Log in with any of them on the start page.

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

**Stop:** `Ctrl+C` stops the application; `docker compose down` stops PostgreSQL and keeps its data (`docker compose down -v` deletes it).
