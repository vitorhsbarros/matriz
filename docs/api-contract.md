# Matriz — API contract (MVP)

This document explains the API specification in [openapi.yaml](../openapi.yaml) (OpenAPI 3.1). It covers every operation the MVP needs (US01–US18): the operations, the design decisions, how they map to the acceptance criteria, and the assumptions behind them.

## 1. Operations

24 operations, grouped by module (ADR-0001).

| Module | Operation | Method and endpoint | US |
|---|---|---|---|
| **Accounts** | Create an account | `POST /users` | US01 |
| | Log in | `POST /session` | US02 |
| | Get the current session | `GET /session` | US02 |
| | Log out | `DELETE /session` | US02 |
| **Projects** | List my projects | `GET /projects` | US04 |
| | Create a project | `POST /projects` | US03 |
| | Get a project | `GET /projects/{projectId}` | US04 |
| | Edit a project | `PATCH /projects/{projectId}` | US14 |
| | Delete a project | `DELETE /projects/{projectId}` | US15 |
| | List the project's members | `GET /projects/{projectId}/members` | US09 |
| | Add a member | `POST /projects/{projectId}/members` | US09 |
| | Remove a member | `DELETE /projects/{projectId}/members/{userId}` | US09 |
| **Teams** | List all teams | `GET /teams` | US11–US13 |
| | Create a team | `POST /teams` | US11 |
| | Rename a team | `PATCH /teams/{teamId}` | US17 |
| | Delete a team | `DELETE /teams/{teamId}` | US18 |
| | Add a member | `POST /teams/{teamId}/members` | US12 |
| | Remove a member | `DELETE /teams/{teamId}/members/{userId}` | US12 |
| **Tasks** | List the project's tasks | `GET /projects/{projectId}/tasks` | US06 |
| | Create a task | `POST /projects/{projectId}/tasks` | US05 |
| | Edit a task or change its status | `PATCH /projects/{projectId}/tasks/{taskId}` | US07, US08 |
| | Delete a task | `DELETE /projects/{projectId}/tasks/{taskId}` | US16 |
| | Assign a task | `PUT /projects/{projectId}/tasks/{taskId}/assignee` | US10, US13 |
| | Undo the assignment | `DELETE /projects/{projectId}/tasks/{taskId}/assignee` | US10 |

## 2. Design decisions

### Resources

- **Tasks and project members are nested under their project** (`/projects/{projectId}/...`). A task belongs to exactly one project for its whole life (I08), and every request inside a project is checked against membership (NFR03).
- **Teams are top-level** (`/teams`), because they exist independently of projects (domain model, Level 1).
- **Singleton sub-resources stay singular:**
  - `/session` is the current user's session;
  - `.../assignee` is the single assignee of a task (I10).
  
  Both are replaced or removed as a whole. Collections are plural.
- **Members are added by email** (D04), so the request body is `{email}`. They are removed by their identifier in the URL.

### Representations

- **Fields:** in `camelCase`, with dates in ISO 8601 (`dueDate` as a date, `createdAt` as a date-time).
- **Assignee as a tagged union:**
  - on input, `{type: "person", userId}` or `{type: "team", teamId}`;
  - on output, the same shape plus the `name` the task list shows (US06).
  
  The `type` field makes "a person *or* a team" explicit and rules out sending both.
- **Role:** responses about projects include the current user's `role` (`MANAGER` or `MEMBER`), which the project list shows (US04). In the data model the role is derived from the manager (database.md, A3).
- **Editing a task with `PATCH`:** one operation covers both editing (US08) and changing the status (US07).
  - Only the fields sent are changed.
  - `null` clears `description` or `dueDate`.
  - The change is all-or-nothing (US08 AC05).

### Status codes

| Code | Meaning in this API |
|---|---|
| `200` | Read, or update that returns the changed resource |
| `201` | Resource created (with `Location` when it has its own URL) |
| `204` | Removed, or logged out, with no body |
| `400` | Body isn't valid JSON, has unknown fields, or has a field of the wrong JSON type |
| `401` | Not logged in, or wrong credentials at log-in |
| `403` | CSRF token invalid, or a member who isn't the manager tries a manager-only action |
| `404` | A resource **in the URL** doesn't exist or isn't accessible |
| `409` | Uniqueness conflict: email, team name, existing membership |
| `422` | Valid structure, but a field is missing or empty, or a value, format, enum or business rule fails |

All errors use Problem Details (RFC 9457) with the exact acceptance criteria message in `detail`.

### Deleting

- **Project deletion** removes the project, its memberships and its tasks in one transaction (FR05). Teams aren't affected.
- **Team deletion** removes the team and its memberships, and unassigns every task assigned to it, in one transaction (FR20). The members keep their accounts and projects.
- **The confirmation before deleting** (NFR05, US15 AC01, US16 AC01, US18 AC01) is shown by the browser application before it calls `DELETE`. The API itself deletes at once. This keeps the API simple, and every client still has to confirm because the user stories require it.

## 3. Mapping to the acceptance criteria

Successful scenarios map to `2xx` responses with the data described in each operation. The tables list the error scenarios and the scenarios that need a specific behaviour.

### Accounts

| US | AC | Response |
|---|---|---|
| US01 | AC01 | `201`, session started (cookie and CSRF token) |
| US01 | AC02, AC03 | `409` — "This email is already in use." |
| US01 | AC04 | `422` — "Name is required." / "Email is required." / "Password is required." |
| US01 | AC05 | `422` — "Enter a valid email address." |
| US01 | AC06 | `422` — "Name must be at most 100 characters." |
| US01 | AC07 | `422` — "Password must be at least 8 characters." / "Password must be at most 128 characters." |
| US01 | AC09, AC10 | `password` is `writeOnly` and never returned. How it is stored is up to the implementation (NFR04). |
| US02 | AC01, AC07 | `200`, session started. Email compared ignoring case. |
| US02 | AC02 | `DELETE /session` → `204`, then any request → `401` "Please log in to continue." |
| US02 | AC03, AC04, AC08, AC09 | `401` — "Invalid email or password." (same for both cases) |
| US02 | AC05 | `422` — "Email and password are required." |
| US02 | AC06 | `401` — "Please log in to continue." on every protected operation |

### Projects

| US | AC | Response |
|---|---|---|
| US03 | AC03 | `422` — "Project name is required." |
| US03 | AC04 | `422` — "Project name must be at most 100 characters." |
| US03 | AC05 | `422` — "Description must be at most 2000 characters." |
| US03 | AC06 | `201`, names can repeat |
| US04 | AC01, AC05 | `200`, only the user's projects, sorted by name ignoring case |
| US04 | AC03 | `200` with an empty list; the interface shows "You are not a member of any project yet." |
| US04 | AC04 | `404` — "Project not found." |
| US09 | AC03 | `204`; the removed member's tasks in the project become unassigned (D10) |
| US09 | AC04 | `422` — "This user could not be added." (doesn't reveal whether the account exists, SEC09) |
| US09 | AC05 | `409` — "This user is already a member of the project." |
| US09 | AC06 | `422` — "The project manager cannot be removed." |
| US09 | AC07 | `403` — "Only the project manager can manage members." |
| US09 | AC10 | `422` — "Email is required." |
| US09 | AC11 | `404` — "Member not found." |
| US14 | AC03, AC06 | `422` — "Project name is required." / "Project name must be at most 100 characters." |
| US14 | AC04 | `403` — "Only the project manager can edit the project." |
| US14 | AC05 | `404` — "Project not found." |
| US15 | AC01, AC03 | Shown by the browser application before calling `DELETE` (NFR05) |
| US15 | AC02 | `204`; the project, its memberships and its tasks are deleted |
| US15 | AC04 | `403` — "Only the project manager can delete the project." |
| US15 | AC05 | `404` — "Project not found." |

### Teams

| US | AC | Response |
|---|---|---|
| US11 | AC02 | `422` — "Team name is required." |
| US11 | AC03, AC04 | `409` — "A team with this name already exists." |
| US11 | AC05 | `422` — "Team name must be at most 50 characters." |
| US12 | AC03 | `422` — "This user could not be added." (doesn't reveal whether the account exists, SEC09) |
| US12 | AC04 | `409` — "This user is already a member of the team." |
| US12 | AC05 | `422` — "The team manager cannot be removed." |
| US12 | AC06 | `403` — "Only the team manager can manage members." |
| US12 | AC09 | `422` — "Email is required." |
| US12 | AC10 | `404` — "Member not found." |
| US12 | AC11 | `404` — "Team not found." |
| US17 | AC02 | `409` — "A team with this name already exists." |
| US17 | AC03, AC07 | `422` — "Team name is required." / "Team name must be at most 50 characters." |
| US17 | AC04 | `403` — "Only the team manager can edit the team." |
| US17 | AC05 | `404` — "Team not found." |
| US18 | AC01, AC03 | Shown by the browser application before calling `DELETE` (NFR05) |
| US18 | AC02 | `204`; the team's memberships are deleted and its tasks become unassigned |
| US18 | AC04 | `403` — "Only the team manager can delete the team." |
| US18 | AC05 | `404` — "Team not found." |
| US12 | AC08 | `201`; existing task assignments to the team are kept (D09) |

### Tasks

| US | AC | Response |
|---|---|---|
| US05 | AC03 | `422` — "Task title is required." |
| US05 | AC04 | `422` — "Due date cannot be in the past." |
| US05 | AC05 | `422` — "Enter a valid date." |
| US05 | AC06 | `404` — "Project not found." |
| US05 | AC08 | `422` — "Task title must be at most 200 characters." / "Description must be at most 5000 characters." |
| US06 | AC01, AC06 | `200`, newest first, including done tasks. The client groups them into the board's status columns, keeping that order within each column. |
| US06 | AC02 | `200` with an empty list; the interface shows "No tasks yet." |
| US07 | AC04 | `422` — "Invalid status." |
| US07 | AC06 | `200`, same status, no error |
| US07 | AC08, US08 AC09 | Last saved change wins (D11) |
| US08 | AC03 | `422` — "Task title is required." |
| US08 | AC04 | `422` — "Due date cannot be in the past." |
| US08 | AC05 | `422`, nothing saved |
| US08 | AC07 | `200`; a past due date is only checked if it changes (D12) |
| US10 | AC03, AC08 | `DELETE .../assignee` → `200`, `assignee: null` |
| US10 | AC04, AC05 | `422` — "The assignee must be a member of the project." |
| US10 | AC07 | `200`, unchanged, no error |
| US13 | AC04 | `422` — "All team members must belong to the project." |
| US13 | AC05 | `422` — "Team not found." |
| US16 | AC01, AC03 | Shown by the browser application before calling `DELETE` (NFR05) |
| US16 | AC02 | `204` |
| US16 | AC04 | `404` — "Project not found." |
| US16 | AC05 | `404` — "Task not found." (DoD05) |

The cross-cutting criteria in the [user stories README](user-stories.md) apply to every operation:

| Criterion | Response |
|---|---|
| DoD02 — not logged in | `401` — "Please log in to continue." |
| DoD03 — not a project member | `404` — "Project not found." (every operation under `/projects/{projectId}`) |
| DoD05 — task doesn't exist | `404` — "Task not found." |
| DoD06 — request can't be verified | `403` — "The request could not be verified. Please reload and try again." |
| DoD07 — malformed request | `400` — "The request body is not valid." |

## 4. Authentication

- **Session cookie:** sign-up and log-in set a session cookie (`MATRIZ_SESSION`) that is `HttpOnly`, `Secure` and `SameSite=Lax`.
- **Where sessions live:** on the server, in the shared database (the SESSION entity in [database.md](database.md)). So:
  - log-out deletes the session and the cookie stops working immediately (US02 AC02);
  - any application instance can serve any request (ADR-0001).
- **CSRF:** sign-up, log-in and `GET /session` return a `csrfToken` in the `Session` body. Every operation that changes data while logged in must send it in the `X-CSRF-Token` header. Sign-up and log-in don't need it, because there is no session yet. Log-out (`DELETE /session`) does need it, because it changes data while logged in.

**Why a cookie and not a token (e.g. JWT):**
- **Log-out:** a token stays valid until it expires, unless a list of revoked tokens is kept. With a cookie, log-out works immediately.
- **Security:** a cookie can be `HttpOnly`, so scripts in the page can't read it, which limits the damage of an XSS bug.
- **Need:** tokens pay off with several kinds of clients or public APIs, and the MVP has neither.
- **Effort:** server-side sessions are the default, lowest-effort option for one developer with 3 weeks.
- **Scalability:** keeping sessions in the shared database meets ADR-0001 (any instance can serve any request) with no extra infrastructure.

This choice settles open point OP2 of the data model and the "session storage" risk in [04-solution-architecture.md](architecture/04-solution-architecture.md). It is recorded in [ADR-0002](architecture/adr/ADR-0002-session-cookie-authentication.md).

## 5. Assumptions

| # | Assumption | Notes |
|---|---|---|
| AS01 | The API is consumed by a client application running in the browser, e.g. a single-page application. | The architecture left the choice between server-rendered pages and a separate client open (03-alternatives, AS05). Designing a JSON API leans towards the separate client. |
| AS02 | Sessions use a server-side session cookie, stored in the shared database. | Decided and recorded in ADR-0002. |
| AS03 | Base URL `/api`, served locally at `http://localhost:8080/api`. | The local server matches the demo context. The linter warns about `localhost`; this is intended. |
| AS04 | Identifiers are UUIDs. | The data model only says identifiers are system-generated (database.md, A1). |
| AS05 | A failed CSRF check returns `403` with "The request could not be verified. Please reload and try again." | Now defined in the acceptance criteria: DoD06. |
| AS06 | Resources that don't exist return `404` with "Task not found.", "Team not found." or "Member not found." | Now defined in the acceptance criteria: DoD05, US09 AC11, US12 AC10, US12 AC11. |
| AS07 | A body that isn't valid JSON, has unknown fields or has a field of the wrong JSON type returns `400` with "The request body is not valid." Missing or empty fields, lengths, formats and enum values return `422` with the acceptance criteria message (e.g. US05 AC03, AC05, US07 AC04). | Now defined in the acceptance criteria: DoD07. Unknown fields are rejected (`additionalProperties: false`). |
| AS08 | Validation errors return `422`, and only the first failing check is reported, in the order the fields are listed in the schema. | The acceptance criteria always show a single message. |
| AS09 | Statuses and roles travel as `TO_DO`, `IN_PROGRESS`, `DONE` and `MANAGER`, `MEMBER`. The interface shows "To do", "Manager", etc. | Matches the data model. |
| AS10 | A team referenced in the body that doesn't exist returns `422`, not `404`. | `404` is kept for resources in the URL. |
| AS11 | "Today" for the due date check (D12) is the current date on the server. | Users in other time zones could see an edge case around midnight. Irrelevant for a local demo. |
| AS12 | Adding a member with no email returns `422` "Email is required.", reusing the US01 message. | Now defined in the acceptance criteria: US09 AC10, US12 AC09. |
| AS13 | Members and teams are listed alphabetically by name. | The user stories only define the order of projects (US04) and tasks (US06). |
| AS14 | There is no "get one task" operation: the task list already returns every field, which is enough to edit a task. | Can be added if the client needs it. |
| AS15 | Empty lists return `200` with `[]`. The messages "You are not a member of any project yet." and "No tasks yet." are shown by the interface. | They describe an empty state, not an error. |
