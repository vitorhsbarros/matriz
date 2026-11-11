## Role
You are a senior Java developer, experienced in Spring Boot 3, PostgreSQL, Flyway and modular monoliths, and in building a React + Vite front end into the same JAR. You are setting up the Matriz codebase, which one developer will keep extending.

## Goal
Prepare the Matriz application and its persistence, so that implementation of the user stories can start:
1. **Project structure:** an executable project, with its dependencies, following the chosen architecture (ADR-0001, the stack in `docs/architecture.md`).
2. **Database and configuration:**
   - the database connection is configured through environment variables;
   - an example file holds fictitious values;
   - real credentials stay out of the repository.
3. **Schema:** created by reproducible migrations, for users, projects, tasks, comments and assignments.
4. **Demo data:**
   - it is reproducible;
   - it has at least two users, and projects that let different access rules be tested;
   - starting the application never deletes data.
5. **Check and commands:** the application starts and reaches the database, and the commands are recorded in `README.md`.
6. **Plan:** `docs/implementation-plan.md` is reviewed to reflect what was done.

## Context
- **Project:** Matriz, a simplified project and task management application. Implementation hasn't started: this is increment I0 of `docs/implementation-plan.md`, plus the schema and the demo data.
- **Artefacts to read:**
  - `AGENTS.md` and `README.md`;
  - `docs/implementation-plan.md`, for I0 and the pending decisions;
  - `docs/architecture.md`, section 5, for the confirmed stack: Java 21, Spring Boot 3, PostgreSQL with Flyway, Testcontainers, React + TypeScript + Vite in the same JAR, Maven;
  - the ADRs in `docs/architecture/adr/`;
  - `docs/database.md`, for the logical data model;
  - `docs/constraints.md`: SEC10, PRI05 and PD12;
  - `openapi.yaml`, as a reference only.
- **Developer's environment:** not known. Check which tools are installed (Java 21, Maven or the wrapper, Docker, Node) and report what is missing before relying on it.
- **Demo accounts (PD12, decided by the user):**
  - three accounts with made-up names: Ana Silva, Rui Costa and Sara Lopes;
  - their passwords are read from environment variables.

## Constraints
- **Scope:**
  - only the project structure, configuration, schema, demo data and start-up;
  - don't implement any user story, endpoint, log-in or screen, beyond an empty start page in the browser application.
- **Dependencies:** list the dependencies with their exact versions and wait for the user's approval before installing them (AGENTS.md §6).
- **Pending decisions:**
  - before writing the schema or the seed, propose options with trade-offs for PD04 (foreign keys between modules), PD03 (module-boundary check) and PD06 (password hashing, which the seed needs);
  - wait for the user's choice.
- **Secrets:**
  - never put real credentials in files, commits or logs (SEC10, AGENTS.md §7);
  - `.env` is listed in `.gitignore`;
  - `.env.example` holds fictitious values only.
- **Demo data:**
  - the seed is idempotent: later start-ups don't duplicate or delete data;
  - all data is made up (PRI05).
- **Commits:** don't commit. Propose a commit message at the end.

## Task
1. **Environment:** check that Java 21, the Maven wrapper (or Maven), Docker and Node are installed. Report what is missing.
2. **Decisions and dependencies:**
   - propose options with trade-offs for PD03, PD04 and PD06;
   - list the dependencies with their exact versions;
   - **stop and wait for the user's approval.**
3. **Project structure:**
   - Maven wrapper and `pom.xml`;
   - the packages `web`, `accounts`, `projects`, `teams` and `tasks`, following ADR-0001;
   - `frontend/` created with Vite (React + TypeScript), built by Maven into the JAR, with an empty start page;
   - `compose.yaml` with PostgreSQL;
   - `.env.example` and `.gitignore`.
4. **Schema:** create Flyway migrations, one folder per module, with the complete schema of `docs/database.md`, following the decision on PD04.
5. **Demo data:** create an idempotent seed with:
   - the three demo accounts (Ana Silva, Rui Costa, Sara Lopes), their passwords read from environment variables and hashed as decided in PD06;
   - projects and memberships that let different access rules be tested (a manager, a member and a non-member).
6. **Start-up check:** start the application against PostgreSQL and confirm it reaches the database.
7. **Commands:** record the commands to start, test and build the application in `README.md` and in `AGENTS.md` §10.
8. **Plan:** update `docs/implementation-plan.md`:
   - the status of I0;
   - the decisions taken;
   - anything left to do.
9. **Report:** report what was done and propose a commit message.

## Validation
Before reporting, check:
1. **Build:** `./mvnw verify` passes, including the front-end build.
2. **Migrations:** a Testcontainers test applies every migration to an empty database, and the schema has the tables of `docs/database.md`.
3. **Idempotent seed:** running the seed twice (or starting the application twice) doesn't change the number of rows.
4. **Distinct access:** the demo data includes at least one manager, one member and one non-member of the same project.
5. **Start-up:** the application starts against the PostgreSQL in `compose.yaml` and responds, e.g. on a health check or the start page.
6. **Secrets:** a search of the repository finds no real password, and `.env` isn't tracked by Git.
7. **Boundaries:** the module-boundary check chosen in PD03 runs in the build.
8. **Report:** include the result of each check. Say what couldn't be checked.
