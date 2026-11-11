# Matriz — Implementation plan (MVP)

- **Status:** in progress. I0, I1 and I2 are done (2026-11-11); I3 is next. The increments and their status are in section 5; update the **Status** column as the work progresses.
- **Scope:** the 23 MVP user stories in [user-stories.md](user-stories.md), after the [scope reconciliation](reviews/scope-reconciliation.md).
- **Prompt:** [docs/prompts/implementation-plan.md](prompts/implementation-plan.md).

Every decision in this plan is marked **Confirmed** (with its source) or **Proposed** (needs the user's approval).

## 1. Analysis

### 1.1 Gaps

| # | Gap | Where | Proposal |
|---|---|---|---|
| G01 | **Technology stack not chosen.** Everything from the build to the tests depends on it. | PD01, AGENTS.md §10 | **Resolved:** confirmed by the user, see section 2. **Proposed:** add the build, test and style commands to AGENTS.md §10 in I0, and close PD01 in constraints.md. |
| G02 | **No operation listed the eligible assignees.** People can be taken from `GET /projects/{projectId}/members` (only members are eligible, US10). Teams can only be worked out by the client, by comparing `GET /teams` with the member list, and the rule (all team members in the project, D09) would then live in two places. | US10, US13, openapi.yaml | **Resolved (user, Q2):** `GET /projects/{projectId}/eligible-assignees` added to `openapi.yaml` (section 4). |
| G03 | **No screens designed for log-in, project members, teams, editing a task and editing a project.** The wireframe covers the home dashboard, the board and the task details only. | docs/ui/README.md, "Out of the wireframe" | **Proposed:** build these screens with the same low-fidelity patterns (forms and tables from the wireframe), without a separate wireframe. |
| G04 | **Demo accounts: number, data and seed mechanism are undefined.** | FR21, US19, PD12, database.md OP5 | Decide with the stack (PD12). **Proposed default:** 3 accounts with made-up names (e.g. Ana Silva, Rui Costa, Sara Lopes, as in the wireframe), passwords read from environment variables at start-up. |
| G05 | **No session expiry.** Sessions last until log-out and abandoned ones stay in the database. | PD02, ADR-0002 | **Resolved:** PD02 decided by the user (section 1.3). |
| G06 | **Authentication flow: what the client does after a `401`.** The contract defines the responses, but not that the client goes to the log-in screen and, after logging in, back where the user was. | US02 AC06, DoD02 | **Resolved in I1:** the client shows the log-in screen with "Please log in to continue." on any `401`; after logging in it opens the home screen (the dashboard from I2, US02 AC01). No API change. |
| G07 | **A deployment is planned, but the documents assume a local demonstration.** ASM10 says the MVP "is demonstrated on a local server", and 04-solution-architecture section 6 only shows a local environment. A deployment needs HTTPS (SEC08), a hosted database, secrets outside the repository (SEC10) and a place to run the seed. | ASM10, 04-solution-architecture §6, SEC08, SEC10 | **Proposed:** update ASM10 to "deployed on one hosting platform, over HTTPS" and add the deployment to 04 §6, once PD13 is decided (needs approval: constraints and architecture changes). The stack already uses PostgreSQL and a container image for this reason. The deployment plan is left for later (user, Q4). |

### 1.2 Contradictions and inconsistencies

| # | Contradiction | Where | Proposal |
|---|---|---|---|
| X01 | **"Technology-independent" vs choosing the stack.** 04-solution-architecture.md says it stays technology-independent, and AGENTS.md §10 says not to assume a stack. Both are correct until PD01 is decided, but must be updated once it is. | 04-solution-architecture.md intro, AGENTS.md §10, README.md "Current status" | After confirmation: keep 04 technology-independent and point to architecture.md section 5; update AGENTS.md §10 and the README status. |
| X02 | **ADR-0001 and ADR-0002 describe the earlier scope** (13 stories; "users sign up"). The decisions themselves still hold. | ADR-0001 Context, ADR-0002 Context and Decision 1 | **Proposed:** leave the ADRs as historical records (they were accepted for that scope) and note in architecture.md that sign-up is now an optional extension. Already noted in docs/architecture/README.md. |
| X03 | **Two architecture choices from the scope reconciliation weren't explicitly confirmed by the user:** the home dashboard is served by the Tasks module, and Projects exposes *Get user names*. | 04-solution-architecture.md §3.4, scope-reconciliation.md "Changes that follow" | **Resolved (user, Q3):** both confirmed. |
| X04 | **A project manager can't delete other people's comments (US22), but deleting the project deletes all its comments (US15).** Not a contradiction in the rules, but it may surprise users. | US15, US22, SEC11 | **Proposed:** keep as it is; note it in the access matrix (section 3). |
| X05 | **The system-architect agent's example** still says "one assignee per task". | .claude/agents/system-architect.md line 75 | **Proposed:** update the example to "several assignees per task" (outside this task; needs approval). |
| X06 | **A broken link in the closed review-01** (`03-alternatives.md` without the `architecture/` folder). | docs/reviews/review-01.md | Report only: closed reviews aren't edited (AGENTS.md §5). |

### 1.3 Pending technical decisions

| PD | Decision | Blocks | Options and trade-offs |
|---|---|---|---|
| PD01 | Technology stack | Everything | **Decided** by the user (section 2). Closing it in constraints.md is proposed. |
| PD02 | Session expiry — **Decided 2026-11-11: (b), 30 minutes without requests** | **I1.** Spring Session expires a session after 30 minutes without use by default, so "sessions last until log-out" (ADR-0002) no longer holds once log-in exists. Decide before I1. | (a) No expiry, as today: simplest; abandoned sessions pile up. (b) Fixed idle timeout (e.g. 8 h): one attribute and a clean-up job. (c) Absolute lifetime: same cost, logs out active users. |
| PD03 | Tool for checking module boundaries — **Decided 2026-11-11: Spring Modulith** | Project set-up | (a) Architecture tests in the test suite (e.g. ArchUnit): plain tests, explicit rules. (b) A module framework (e.g. Spring Modulith): checks plus event support, more framework to learn. (c) Package conventions only: no tool, relies on discipline (ADR-0001 asks for an automated check). |
| PD04 | Foreign keys between modules' data — **Decided 2026-11-11: foreign keys with `ON DELETE RESTRICT`** | Project set-up (schema) | (a) Foreign keys: the database protects integrity; deletes must follow module order. (b) Identifiers only: modules fully independent; integrity relies on the events (R12, R17, R19). |
| PD05 | Assign and remove a member at the same time | Assigning people (US10) | (a) Lock the membership row while assigning: simple, short waits. (b) Serializable transactions for these two operations: correct, retries needed. (c) Accept the rare race for the demo, documented as a known risk. |
| PD06 | Password hashing — **Decided 2026-11-11: BCrypt** | Log-in (US02, US19) | (a) BCrypt: the framework default, well known. (b) Argon2: stronger, needs an extra library. Both meet NFR04. |
| PD07 | Deleting an account | Not in the MVP | Deferred (constraints.md). |
| PD08 | Scalability as a requirement | Not in the MVP | Deferred. |
| PD09 | Review-01 F05 wording | Nothing | Wording only; decide when convenient. |
| PD10 | Repeated log-in attempts | Not in the MVP | (a) None in the MVP: demo with seed accounts. (b) Temporary block per email after N failures: small, but must not reveal whether the email exists (SEC04). |
| PD11 | Who can see teams | Teams (US11–US13) | (a) Everyone (D05, as today): simplest. (b) Only users sharing a project with the team's members: more private, more queries. |
| PD12 | Demo accounts — **Decided 2026-11-11:** 3 accounts and 2 projects, seed at start-up | Log-in (US02, US19) | See G04. In the deployment, the seed runs at start-up (e.g. a Flyway migration or a start-up task) and reads the passwords from the platform's secrets. |
| PD13 | **Deployment platform** (new) | The deployment, planned later | (a) A platform that runs a container and offers managed PostgreSQL (e.g. Render, Railway, Fly.io): least set-up, HTTPS included, free tiers may sleep or expire. (b) A virtual machine with Docker Compose: full control, but HTTPS, updates and backups are the developer's job. (c) A university or company server, if one is available: depends on what it offers. |

### 1.4 Questions for the user

| # | Question | Answer |
|---|---|---|
| Q1 | Confirm the proposed stack? | Yes, after two changes: PostgreSQL instead of H2 (a deployment is planned), and a React single-page application. |
| Q2 | Add `GET /projects/{projectId}/eligible-assignees`? | Yes. |
| Q3 | Confirm the dashboard served by Tasks and *Get user names* in Projects? | Yes. |
| Q4 | Where will the application be deployed? | Left for later. The plan below doesn't include the deployment. |

## 2. Technology stack

**Confirmed** by the user on 2026-11-11 (PD01). Details and alternatives in [architecture.md, section 5](architecture.md#5-technology-stack).

| Part | Choice |
|---|---|
| Language | Java 21 |
| Application | Spring Boot 4.1.2 (moved from 3 at the start of I0, user's decision): Spring Web MVC, Spring Security (session cookie, CSRF header), Spring Session JDBC, Spring Data JPA |
| Database | PostgreSQL, with Flyway migrations (one folder per module); Docker Compose locally, Testcontainers in tests |
| Browser application | React + TypeScript, built with Vite, served from the same JAR |
| Build and tests | Maven; JUnit 5, Spring Boot Test with MockMvc; Vitest and Testing Library |
| Packaging | One executable JAR in one container image |

The layout below was created in I0:

```
matriz/
├── pom.xml                          Maven build (back end + browser application)
├── compose.yaml                     PostgreSQL for local development
├── .env.example                     configuration with fictitious values (copy to .env, never committed)
├── src/main/java/matriz/
│   ├── web/                         web entry: session, CSRF, errors (Problem Details)
│   ├── accounts/                    Accounts module
│   ├── projects/                    Projects module
│   ├── teams/                       Teams module
│   └── tasks/                       Tasks module (tasks, assignees, comments, dashboard)
├── src/main/resources/db/migration/ Flyway migrations, one folder per module
├── src/test/java/matriz/            ModularityTest (PD03), PersistenceSetupTest, StartUpTest
└── frontend/                        React + TypeScript (Vite)
    └── src/pages/                   log-in, home dashboard, board, task details, members, teams
```

Each module's public interface is the types directly in its package (e.g. `matriz.accounts.AccountDirectory`); everything in its sub-packages (e.g. `matriz.accounts.internal`) is private to it. Spring Modulith checks this in every build. The three layers of ADR-0001 go inside the module's sub-packages as the code grows.

## 3. Access rules

The matrix consolidates who can do what. **Confirmed** cells give their source; **Proposed** cells fill a gap and need approval.

**Roles:**
- **Visitor:** not logged in.
- **User:** logged in, not a member of the project or team in question.
- **Member:** member of the project (or team), not its manager.
- **Manager:** manager of the project (or team). Always also a member (D07).
- **Author:** the member who wrote a comment.

### 3.1 Projects

| Action | Visitor | User | Member | Manager | Source |
|---|---|---|---|---|---|
| See the home dashboard and search own projects | No | Yes (own projects only) | Yes | Yes | Confirmed: FR07, FR24, US04, US23 |
| Create a project (becomes its manager) | No | Yes | Yes | Yes | Confirmed: FR03, D07 |
| Open a project and its board | No | No: "Project not found." | Yes | Yes | Confirmed: NFR03, DoD03, SEC03 |
| See the member list (names, emails, roles) | No | No | Yes | Yes | Confirmed: US09, PRI03 |
| Edit or delete the project | No | No: "Project not found." | No: "Only the project manager can …" | Yes, delete after confirming | Confirmed: FR04, FR05, NFR03, NFR05, US14, US15 |
| Add or remove members | No | No | No | Yes; can't remove themself | Confirmed: FR06, D07, US09 |

### 3.2 Tasks

| Action | Visitor | User | Member | Manager | Source |
|---|---|---|---|---|---|
| See tasks, task details, filter the board | No | No: "Project not found." | Yes | Yes | Confirmed: NFR03, US06, US20, US24 |
| Create, edit, change status, change priority | No | No | Yes, any task | Yes, any task | Confirmed: D08, US05, US07, US08 |
| Delete a task (after confirming) | No | No | Yes, any task | Yes, any task | Confirmed: D08, NFR05, US16 |
| See the eligible assignees | No | No | Yes | Yes | Confirmed: US10, US13; operation added (Q2) |
| Assign or remove people and teams | No | No | Yes, any task | Yes, any task | Confirmed: D08, FR14, US10, US13 |
| Be assigned | — | No: only project members | Yes | Yes | Confirmed: FR14, US10 AC04 |
| A team can be assigned | — | — | If all its members are project members, at that moment | Same | Confirmed: D09, US13 AC04 |

### 3.3 Comments

| Action | Visitor | User | Member | Manager | Author | Source |
|---|---|---|---|---|---|---|
| Read a task's comments | No | No: "Project not found." | Yes | Yes | Yes | Confirmed: FR23, US21 |
| Add a comment | No | No | Yes | Yes | — | Confirmed: FR23, US21 |
| Edit or delete a comment | No | No | No: "Only the author can change this comment." | No, same message | Yes (delete after confirming) | Confirmed: SEC11, NFR03, NFR05, US22 |
| A comment's author left the project | — | — | Comment stays, with the name | Same | Can no longer edit or delete it (no access) | Confirmed: D13. The last cell is **Proposed**: it follows from NFR03 but no criterion says it. |
| Comments when the project is deleted | — | — | — | Deleted with the project, including other people's | — | Confirmed: FR05, US15. See X04. |

### 3.4 Teams

| Action | Visitor | User | Member (team) | Manager (team) | Source |
|---|---|---|---|---|---|
| See all teams and their members' names | No | Yes | Yes | Yes | Confirmed: D05, PRI04. PD11 still open. |
| Create a team (becomes its manager) | No | Yes | Yes | Yes | Confirmed: FR08, D07 |
| Rename, delete (after confirming), add or remove members | No | No: "Only the team manager can …" | No | Yes; can't remove themself | Confirmed: FR09, FR19, FR20, NFR03, NFR05, D07 |

Team membership gives **no** access to projects: only project membership does (US12 AC08, NFR03). Confirmed.

### 3.5 Checks against the rules

| Rule | How the matrix respects it |
|---|---|
| NFR03, SEC02 | Only members see or change a project's tasks and comments; manager-only actions are refused to members. |
| SEC11 | Only the author edits or deletes a comment, not even the manager. |
| D07 | Managers are members, and can't be removed. |
| D08 | Any member can change, assign and delete any task, not only assignees. |

## 4. API contract

The contract in [openapi.yaml](../openapi.yaml) and [api-contract.md](api-contract.md) has **30 operations**. Changes for this plan:

| Change | Status |
|---|---|
| `GET /projects/{projectId}/eligible-assignees` (`listEligibleAssignees`), served by Tasks: project members, and teams whose members are all in the project. | Added. Confirmed by the user (Q2). |
| **Authentication flow:** `POST /session` (log in, returns the CSRF token), `GET /session` (current user and token after a reload), `DELETE /session` (log out, needs the token). No operation is missing: demo accounts are created by the seed, not through the API (US19). | Checked, no change. The client's behaviour on `401` is G06 (Proposed). |

## 5. Increments

Nine increments, in about 15 working days (ASM08). Each one ends with a working, demonstrable application. The order follows the backlog where possible; the differences are explained under the table.

**Estimates and the order are Proposed.** The stories, their acceptance criteria and the architecture they follow are Confirmed (user-stories.md, ADR-0001, ADR-0002).

| # | Increment | Stories | Depends on | Days | Status |
|---|---|---|---|---|---|
| I0 | Project set-up | — | — | 1 | Done |
| I1 | Access: demo accounts, log-in and log-out | US19, US02 | I0 | 1.5 | Done |
| I2 | Project management: create, home dashboard, search, edit, delete | US03, US04, US23, US14, US15 | I1 | 3 | Done |
| I3 | Tasks and the board | US05, US06 | I2 | 1 | Not started |
| I4 | Working on a task: status, editing, details, deleting | US07, US08, US20, US16 | I3 | 2 | Not started |
| I5 | Project members and assigning people | US09, US10 | I4 | 2 | Not started |
| I6 | Teams and assigning teams | US11, US12, US13, US17, US18 | I5 | 2.5 | Not started |
| I7 | Comments | US21, US22 | I4 | 1.5 | Not started |
| I8 | Board filters | US24 | I3 | 0.5 | Not started |
| | **Total** | **23 stories** | | **15** | |

**Order changed on 2026-11-11** (user's decision, [docs/prompts/project-management.md](prompts/project-management.md)): the whole project management comes first, as the first complete path between the interface, the API and the database. US14 and US15 moved from the old I6, US23 from the old I8, and US05 and US06 moved from I2 to I3. The later increments were renumbered (old I3–I5 are now I4–I6). The total stays 15 days.

**Differences from the backlog order:**
- **US04 before US05:** the dashboard is built with the projects. Its task counts are zero until tasks exist (I3); the counts were tested with tasks written straight into the database.
- **US20 and US16 in I4:** the task details screen is where editing (US08) and deleting happen, so they're built together.
- **US14 and US15 with the other project operations (I2),** instead of after the teams.

**Risk:** the 15 days leave no margin (ASM08). If time runs short, the increment that can move to the end or be cut first is I8, which doesn't block any other increment.

### I0 — Project set-up

| | |
|---|---|
| **Components and files** | `pom.xml` with the Maven wrapper; Spring Boot application; the four module packages and `web` (section 2); `compose.yaml` with PostgreSQL; Flyway set-up with one folder per module; `frontend/` created with Vite (React + TypeScript), built by Maven into the JAR; Vite proxy for `/api`; Problem Details error handler (`400` "The request body is not valid.", DoD07). |
| **Dependencies** | None. Needs PD03 (boundary check) and PD04 (foreign keys) decided. |
| **Acceptance criteria** | The application starts locally against PostgreSQL; one `mvn verify` builds the back end and the browser application into one JAR; the browser application is served from the same site as `/api`; a malformed request returns DoD07's message. |
| **Checks** | `mvn verify` passes; a boundary test fails when a module uses another module's internals (PD03); a test for DoD07. AGENTS.md §10 gets the build, test and style commands (Proposed follow-up). |

**Done on 2026-11-11.** The prompt is in [docs/prompts/application-setup.md](prompts/application-setup.md).

| Item | Result |
|---|---|
| Project structure | `pom.xml` (Spring Boot 4.1.2, Spring Modulith 2.1.2), Maven wrapper, packages `web`, `accounts`, `projects`, `teams`, `tasks`; `frontend/` (React 19.3.1, TypeScript 5.9.3, Vite 8.3.5, Vitest 4.1.12) built by Maven into the JAR. |
| Configuration | `application.yaml` reads every secret from environment variables; `.env` (ignored by Git) is loaded for local development; `.env.example` has fictitious values only. The session cookie is `MATRIZ_SESSION`, `HttpOnly`, `Secure`, `SameSite=Lax` (ADR-0002). |
| Schema | Flyway migrations for the **whole** data model, one folder per module: `users` and the Spring Session tables (Accounts), `project`, `project_member` (Projects), `team`, `team_member` (Teams), `task`, `task_assignee_user`, `task_assignee_team`, `task_comment` (Tasks). |
| Demo data | Seed at start-up when `MATRIZ_DEMO_SEED=true`: Ana Silva, Rui Costa, Sara Lopes; *Website* (Ana manager, Rui member, Sara not a member) and *App* (Rui manager, Ana member). Only missing rows are inserted; nothing is changed or deleted. |
| Access | Only the start page, its files and `/actuator/health` are public; every other request gets `403` until log-in exists (I1). |
| Commands | In [README.md](../README.md#running-the-application) and AGENTS.md §10. |
| Checks | `./mvnw verify`: 7 back-end tests and 1 front-end test pass. Started twice against the PostgreSQL in `compose.yaml`: health `UP`, start page `200`, same row counts after the second start. |

**Differences from the plan above:**
- **DoD07 (Problem Details for a malformed body) moved to I1**, because I0 has no API operation to test it with.
- **Physical names differ from database.md** where a name is reserved in SQL: USER is `users`, COMMENT is `task_comment`. SESSION is stored in Spring Session's tables (`spring_session`, `spring_session_attributes`), with the CSRF token as a session attribute. **Proposed:** add a note on these names to database.md.
- **The Teams and Tasks tables were created now**, with the whole schema, as the prompt asked; their behaviour still arrives in I2–I7.

**Left for later:**
- PD02 (session expiry) must be decided before I1, see section 1.3. **Decided 2026-11-11.**
- On the developer's machine, another program uses port 8080, so the check ran on port 8081 (`SERVER_PORT=8081`). The Vite proxy assumes 8080.

### I1 — Access: demo accounts, log-in and log-out (US19, US02)

| | |
|---|---|
| **Components and files** | `accounts`: USER and SESSION, password hashing (PD06), seed of the demo accounts (PD12), passwords from environment variables. `web`: session resolution, CSRF check (ADR-0002). Spring Session JDBC table. Operations: `logIn`, `getSession`, `logOut`. Browser application: log-in screen, header with the user's name and **Log out**, redirect to log-in on `401` (G06). |
| **Dependencies** | I0. PD06 and PD12 decided. |
| **Acceptance criteria** | US19 AC01–AC03; US02 AC01–AC10; DoD02, DoD06. Exact messages: "Invalid email or password.", "Email and password are required.", "Please log in to continue.", "The request could not be verified. Please reload and try again." |
| **Checks** | API tests per AC; AC09 checks both messages are identical; a test that the stored password isn't readable (US19 AC03); a search of the repository for the demo passwords finds nothing (US19 AC02, SEC10); the cookie has `HttpOnly`, `Secure` and `SameSite=Lax`. |

**Done on 2026-11-11.** The prompt is in [docs/prompts/authentication-and-access.md](prompts/authentication-and-access.md).

| Item | Result |
|---|---|
| Log-in, current user, log-out | `POST`, `GET` and `DELETE /api/session` in Accounts (`SessionController`). Log-in renews the session identifier and the CSRF token; the session keeps only the user's identifier. Unknown email and wrong password get the same answer, and the password is checked against a dummy hash when the email is unknown. |
| Web entry | `SecurityConfig`: public files, health check and log-in; everything else needs a session (`401` "Please log in to continue."); CSRF token in `X-CSRF-Token` on every change (`403` "The request could not be verified. Please reload and try again."). Errors as Problem Details; DoD07 (`400` "The request body is not valid.", with unknown fields and wrong JSON types refused). |
| Access check | `matriz.projects.ProjectAccess` (public interface of Projects, *Is member*): `requireMember` gives `404` "Project not found." to non-members, `requireManager` gives `403` with the story's message (`ManagerOnlyAction`) to members who aren't the manager. The user always comes from the session. Used by every project operation from I2. |
| Session expiry | PD02: 30 minutes without requests (`application.yaml`). |
| Browser application | Log-in screen; header with the user's name and **Log out**; on any `401`, back to log-in with "Please log in to continue."; the CSRF token in memory only. The Vite proxy port is configurable (`MATRIZ_API_PORT`). |
| Documentation | Session lifecycle in [api-contract.md, section 4](api-contract.md#4-authentication) and README.md; PD02 in constraints.md and openapi.yaml. |
| Checks | `./mvnw verify`: 26 back-end tests (`SessionApiTest` 14, `ProjectAccessTest` 5, and the earlier 7) and 5 front-end tests pass, with `ModularityTest`. Manual check on port 8081, with curl and in a browser, as Ana and as Sara. |

**Differences from the plan above:**
- **The access check is tested through a test-only endpoint** (`ProjectAccessTest`), because the project operations arrive in I2, I4 and I6. With Ana, Rui and Sara it checks member access, `404` for a non-member, `403` for a member on a manager-only action, and that a user identifier in the request is ignored.
- **The home screen is a placeholder** with the header only; US02 AC01 ("the home dashboard is shown") is complete when US04 arrives in I2.
- **Without a valid session, `401` comes before the CSRF check** (approved by the user): after expiry or log-out, a change request gets "Please log in to continue.", not "could not be verified". See api-contract.md, section 4.
- **No new dependencies:** the back-end tests use real HTTP requests instead of `spring-security-test`.

**Left for later:**
- **The local `.env` still has the example passwords** copied from `.env.example` in I0, so the search of the repository for the demo passwords (US19 AC02) finds `.env.example`. Replace them in `.env`; the check then finds nothing.
- ~~PD02 is still described as open in database.md, 04-solution-architecture.md and architecture.md.~~ Updated on 2026-11-11, at the user's request.

### I2 — Project management: create, home dashboard, search, edit, delete (US03, US04, US23, US14, US15)

| | |
|---|---|
| **Components and files** | `projects`: `createProject`, `getProject`, `updateProject`, `deleteProject`; *Get my projects* (`MyProjects`) with the name search; the *project deleted* event (`ProjectDeleted`). `tasks`: `listMyProjects`, the home dashboard with task counts and totals (served by Tasks, confirmed Q3); handler of *project deleted* (R17). Browser application: home dashboard with counts, totals and **Search projects**, **New project**, the project screen with **Edit** and **Delete project** for the manager, the delete confirmation. |
| **Dependencies** | I1. |
| **Acceptance criteria** | US03 AC01–AC06; US04 AC01, AC03–AC09; US23 AC01–AC05; US14 AC01–AC08; US15 AC01–AC07; DoD02–DoD04, DoD06, DoD07. |
| **Checks** | API tests per AC; a non-member gets "Project not found." on every operation (DoD03); the totals equal the sum of the projects and don't change with a search (US04 AC07, D14); deleting a project leaves no rows behind (R17); the delete confirmation text is exact (NFR05). |

**Done on 2026-11-11.** The prompt is in [docs/prompts/project-management.md](prompts/project-management.md).

| Item | Result |
|---|---|
| Projects | `ProjectsController` and `JdbcProjects`: create (the creator becomes manager and member, from the session), get, edit (only the fields sent; `null` clears the description) and delete, with the exact messages of US03 and US14. Access through `ProjectAccess` (I1). |
| Home dashboard | `DashboardController` in Tasks: the user's projects from `MyProjects`, sorted by name ignoring case, with `taskCounts`; `totals` always over every project (D14). The search ignores case and spaces and takes `%` and `_` literally. |
| Deleting | One transaction: `ProjectDeleted` is published first, and the Tasks handler deletes the project's tasks (with their assignees and comments, by cascade); then the project is deleted, with its memberships. Teams are left as they are. |
| Browser application | Home dashboard (wireframe screen 1), project screen (screen 2 without the board), the forms and the confirmation. The screen is kept in the address (`#/projects/<id>`), so a reload stays on it. |
| Checks | `./mvnw verify`: 66 back-end tests (`ProjectsApiTest` 24, `DashboardApiTest` 16, and the earlier 26) and 16 front-end tests pass, with `ModularityTest`. Manual check on port 8081 in a browser: log in as Ana, create a project, see it in the list, reload, restart the back end, see it again; Sara doesn't see it and gets "Project not found." at its address; Ana deletes it after the confirmation. |

**Differences from the plan above:**
- **Gaps resolved with the user's approval:**
  - an identifier in the URL that isn't a UUID gets `404` "Project not found.";
  - `PATCH` with `{}` changes nothing and returns `200` (US14 AC07);
  - an empty or blank description is stored and returned as `null`;
  - `"name": null` in `PATCH` gets "Project name is required.".
- **US04 AC02 (open the board):** clicking a project opens the project screen; the board arrives with US06 in I3.
- **Delete is offered on the dashboard too,** as in the wireframe, with the same confirmation.
- **No new dependencies:** navigation uses the address's `#`, without a router library.
- **Tests write memberships and tasks straight into the database,** because their operations arrive in I3 and I5.

### I3 — Tasks and the board (US05, US06)

| | |
|---|---|
| **Components and files** | `tasks`: TASK with status and priority, `createTask`, `listProjectTasks` (without filters yet). Browser application: the board with three columns and cards on the project screen, **New task**. |
| **Dependencies** | I2. |
| **Acceptance criteria** | US05 AC01–AC10; US06 AC01–AC06; US04 AC02 (open the board). |
| **Checks** | API tests per AC; a non-member gets "Project not found." (DoD03); the dashboard counts follow the tasks created (US04 AC07); the screen matches the wireframe's screen 2. |

### I4 — Working on a task: status, editing, details, deleting (US07, US08, US20, US16)

| | |
|---|---|
| **Components and files** | `tasks`: `updateTask` (status, fields, priority), `getTask`, `deleteTask` (R22). Browser application: drag and drop and ← → on the board, task details screen (wireframe screen 3, without comments and assignees yet), edit form, delete with confirmation. |
| **Dependencies** | I3. |
| **Acceptance criteria** | US07 AC01–AC08; US08 AC01–AC10; US20 AC01–AC04; US16 AC01–AC07; DoD05. |
| **Checks** | API tests per AC; last saved change wins (D11); a deleted task gets "Task not found." (DoD05); the confirmation text is exact (NFR05). |

### I5 — Project members and assigning people (US09, US10)

| | |
|---|---|
| **Components and files** | `projects`: `listProjectMembers`, `addProjectMember`, `removeProjectMember`, the *member removed* event (D10), *Get members*, *Get user names*. `tasks`: TASK_ASSIGNEE_USER, `addTaskAssignee` (people), `removeTaskUserAssignee`, `listEligibleAssignees` (people), handler of *member removed* (R12). Browser application: members screen, assignees on the task details and the cards. |
| **Dependencies** | I4. PD05 decided. |
| **Acceptance criteria** | US09 AC01–AC11; US10 AC01–AC10; SEC09 ("This user could not be added."). |
| **Checks** | API tests per AC; removing a member and its tasks' assignees happen in one transaction, and a failing handler rolls both back (04-solution-architecture §4.2); a test for the concurrency rule chosen in PD05. |

### I6 — Teams and assigning teams (US11, US12, US13, US17, US18)

| | |
|---|---|
| **Components and files** | `teams`: TEAM, TEAM_MEMBER, `listTeams`, `createTeam`, `renameTeam`, `deleteTeam`, `addTeamMember`, `removeTeamMember`, the *team deleted* event. `tasks`: TASK_ASSIGNEE_TEAM, team assignment in `addTaskAssignee`, `removeTaskTeamAssignee`, teams in `listEligibleAssignees` (D09), handler of *team deleted* (R19). Browser application: teams screen. |
| **Dependencies** | I5. PD11 decided, or D05 kept. |
| **Acceptance criteria** | US11 AC01–AC05; US12 AC01–AC11; US13 AC01–AC09; US17 AC01–AC07; US18 AC01–AC07. |
| **Checks** | API tests per AC; team names are unique ignoring case and spaces (D06); team membership gives no project access (US12 AC08). |

### I7 — Comments (US21, US22)

| | |
|---|---|
| **Components and files** | `tasks`: COMMENT, `listTaskComments`, `addTaskComment`, `editTaskComment`, `deleteTaskComment`; authors' names through *Get user names* (D13). Browser application: comments on the task details screen, edit and delete for the author's own comments. |
| **Dependencies** | I4 (task details). Uses *Get user names* from I5. |
| **Acceptance criteria** | US21 AC01–AC07; US22 AC01–AC08; SEC11. |
| **Checks** | API tests per AC; the manager gets "Only the author can change this comment." (US22 AC05); a removed member's comment keeps the name (US21 AC06). |

### I8 — Board filters (US24)

| | |
|---|---|
| **Components and files** | `tasks`: `?status=` and `?priority=` on `listProjectTasks`. Browser application: filter chips on the board. The dashboard search (US23) was done in I2. |
| **Dependencies** | I3. |
| **Acceptance criteria** | US24 AC01–AC06. |
| **Checks** | API tests per AC; an invalid filter value returns "Invalid priority." or "Invalid status." |

**I5 is listed before I7, but I7 needs only I4 and *Get user names*.** If I7 is done before I5, *Get user names* is built in I7 instead.

## 6. Operations per increment

| Increment | Operations |
|---|---|
| I1 | `logIn`, `getSession`, `logOut` |
| I2 | `createProject`, `getProject`, `updateProject`, `deleteProject`, `listMyProjects` (with `name`) |
| I3 | `createTask`, `listProjectTasks` |
| I4 | `updateTask`, `getTask`, `deleteTask` |
| I5 | `listProjectMembers`, `addProjectMember`, `removeProjectMember`, `listEligibleAssignees`, `addTaskAssignee`, `removeTaskUserAssignee` |
| I6 | `listTeams`, `createTeam`, `renameTeam`, `deleteTeam`, `addTeamMember`, `removeTeamMember`, `removeTaskTeamAssignee` |
| I7 | `listTaskComments`, `addTaskComment`, `editTaskComment`, `deleteTaskComment` |
| I8 | (extends `listProjectTasks`) |

## 7. Keeping this plan up to date

- When an increment starts, set its **Status** to "In progress"; when all its checks pass, set it to "Done".
- When a pending decision is taken (PD03–PD06, PD11–PD13), record it where it belongs (an ADR, constraints.md or architecture.md) and update section 1.3.
- Changes to the order or the estimates are recorded here, with the reason.
