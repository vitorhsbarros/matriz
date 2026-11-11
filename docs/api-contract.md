# Matriz — API contract (MVP)

This document explains the API specification in [openapi.yaml](../openapi.yaml) (OpenAPI 3.1). It covers every operation the MVP needs (US02–US24): the operations, the design decisions, how they map to the acceptance criteria, and the assumptions behind them.

## 1. Operations

30 operations, grouped by module (ADR-0001). Public registration (US01, `POST /users`) was removed when it became an optional extension ([scope reconciliation](reviews/scope-reconciliation.md), SC01); its design is kept in [section 6](#6-optional-extensions).

| Module | Operation | Method and endpoint | US |
|---|---|---|---|
| **Accounts** | Log in | `POST /session` | US02 |
| | Get the current session | `GET /session` | US02 |
| | Log out | `DELETE /session` | US02 |
| **Projects** | Get the home dashboard (with search) | `GET /projects?name=` | US04, US23 |
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
| **Tasks** | List who can be assigned the project's tasks | `GET /projects/{projectId}/eligible-assignees` | US10, US13 |
| | List the project's tasks (with filters) | `GET /projects/{projectId}/tasks?status=&priority=` | US06, US24 |
| | Create a task | `POST /projects/{projectId}/tasks` | US05 |
| | Get a task's details | `GET /projects/{projectId}/tasks/{taskId}` | US20 |
| | Edit a task or change its status | `PATCH /projects/{projectId}/tasks/{taskId}` | US07, US08 |
| | Delete a task | `DELETE /projects/{projectId}/tasks/{taskId}` | US16 |
| | Assign a person or a team | `POST /projects/{projectId}/tasks/{taskId}/assignees` | US10, US13 |
| | Remove a person from the assignees | `DELETE /projects/{projectId}/tasks/{taskId}/assignees/users/{userId}` | US10 |
| | Remove a team from the assignees | `DELETE /projects/{projectId}/tasks/{taskId}/assignees/teams/{teamId}` | US13 |
| | List a task's comments | `GET /projects/{projectId}/tasks/{taskId}/comments` | US21 |
| | Comment on a task | `POST /projects/{projectId}/tasks/{taskId}/comments` | US21 |
| | Edit my comment | `PATCH /projects/{projectId}/tasks/{taskId}/comments/{commentId}` | US22 |
| | Delete my comment | `DELETE /projects/{projectId}/tasks/{taskId}/comments/{commentId}` | US22 |

## 2. Design decisions

### Resources

- **Tasks and project members are nested under their project** (`/projects/{projectId}/...`). A task belongs to exactly one project for its whole life (I08), and every request inside a project is checked against membership (NFR03).
- **Teams are top-level** (`/teams`), because they exist independently of projects (domain model, Level 1).
- **Singleton sub-resources stay singular:** `/session` is the current user's session. Collections are plural.
- **Assignees are a collection** (`.../assignees`), because a task can have several (FR14). One is added at a time with `POST`. People and teams are removed under separate paths (`.../assignees/users/{userId}`, `.../assignees/teams/{teamId}`), so each identifier is unambiguous.
- **Comments are nested under their task** (`.../tasks/{taskId}/comments`). They belong to the Tasks module and are only reachable by project members (NFR03).
- **Eligible assignees have their own read operation** (`.../eligible-assignees`), served by Tasks. It applies the eligibility rules (project members; teams whose members are all in the project, D09) in one place, the server, so the client doesn't repeat them. `addTaskAssignee` still checks them again (US10 AC05). Added for the [implementation plan](implementation-plan.md).
- **Search and filters are query parameters** on the existing lists: `?name=` on the dashboard (US23), and `?status=` and `?priority=` on the tasks (US24), each repeatable.
- **Members are added by email** (D04), so the request body is `{email}`. They are removed by their identifier in the URL.

### Representations

- **Fields:** in `camelCase`, with dates in ISO 8601 (`dueDate` as a date, `createdAt` as a date-time).
- **Assignees as a list of tagged items:**
  - on input, one item at a time: `{type: "person", userId}` or `{type: "team", teamId}`;
  - on output, `assignees` is a list of the same shapes plus the `name` the board shows (US06). An empty list means "Unassigned".

  The `type` field makes each item "a person *or* a team" explicit.
- **Priority:** travels as `LOW`, `MEDIUM`, `HIGH`, like the status. It is optional when creating (`MEDIUM` by default).
- **Home dashboard:** `GET /projects` returns `{totals, projects}`. Each project carries its `taskCounts` per status, and `totals` adds them up across every project of the user, whatever the search (D14). These are plain counts (C02).
- **Comments:** each has its `author` (identifier and name), `createdAt`, and `editedAt` (`null` until edited; when set, the interface shows "(edited)").
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
| `403` | CSRF token invalid, a member who isn't the manager tries a manager-only action, or someone who isn't the author tries to change a comment |
| `404` | A resource **in the URL** doesn't exist or isn't accessible |
| `409` | Uniqueness conflict: email, team name, existing membership |
| `422` | Valid structure, but a field is missing or empty, or a value, format, enum or business rule fails |

All errors use Problem Details (RFC 9457) with the exact acceptance criteria message in `detail`.

### Deleting

- **Project deletion** removes the project, its memberships and its tasks, with their assignees and comments, in one transaction (FR05). Teams aren't affected.
- **Task deletion** removes the task with its assignees and comments.
- **Team deletion** removes the team and its memberships, and removes it from the assignees of every task, in one transaction (FR20). The tasks' other assignees stay, and the members keep their accounts and projects.
- **The confirmation before deleting** (NFR05, US15 AC01, US16 AC01, US18 AC01, US22 AC02) is shown by the browser application before it calls `DELETE`. The API itself deletes at once. This keeps the API simple, and every client still has to confirm because the user stories require it.

## 3. Mapping to the acceptance criteria

Successful scenarios map to `2xx` responses with the data described in each operation. The tables list the error scenarios and the scenarios that need a specific behaviour.

### Accounts

| US | AC | Response |
|---|---|---|
| US19 | AC01 | No operation of its own: the seed creates the accounts, and `POST /session` logs in with them |
| US02 | AC01, AC07 | `200`, session started. Email compared ignoring case. |
| US02 | AC02 | `DELETE /session` → `204`, then any request → `401` "Please log in to continue." |
| US02 | AC03, AC04, AC08, AC09 | `401` — "Invalid email or password." (same for both cases) |
| US02 | AC05 | `422` — "Email and password are required." |
| US02 | AC06 | `401` — "Please log in to continue." on every protected operation |
| US02 | AC10 | `GET /session` → `200` with the user's `name` |

### Projects

| US | AC | Response |
|---|---|---|
| US03 | AC03 | `422` — "Project name is required." |
| US03 | AC04 | `422` — "Project name must be at most 100 characters." |
| US03 | AC05 | `422` — "Description must be at most 2000 characters." |
| US03 | AC06 | `201`, names can repeat |
| US04 | AC01, AC05 | `200`, only the user's projects, sorted by name ignoring case |
| US04 | AC03 | `200` with an empty `projects` list; the interface shows "You are not a member of any project yet." |
| US04 | AC04 | `404` — "Project not found." |
| US04 | AC07, AC08 | `200`; each project has its `taskCounts`, and `totals` adds them up |
| US23 | AC01, AC03 | `GET /projects?name=web` → `200`, only the user's projects whose name contains the text |
| US23 | AC02 | `200` with an empty `projects` list; the interface shows "No projects match your search." |
| US23 | AC05 | `totals` doesn't depend on `name` (D14) |
| US09 | AC03 | `204`; the removed member is removed from the assignees of the project's tasks (D10) |
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
| US15 | AC02 | `204`; the project, its memberships and its tasks (with their comments) are deleted |
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
| US18 | AC02 | `204`; the team's memberships are deleted and the team is removed from its tasks' assignees |
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
| US05 | AC02 | `201`, `priority: MEDIUM` when not sent |
| US05 | AC10, US08 AC10 | `422` — "Invalid priority." |
| US06 | AC01, AC06 | `200`, newest first, including done tasks. The client groups them into the board's status columns, keeping that order within each column. |
| US06 | AC02 | `200` with an empty list; the interface shows "No tasks yet." |
| US07 | AC04 | `422` — "Invalid status." |
| US07 | AC06 | `200`, same status, no error |
| US07 | AC08, US08 AC09 | Last saved change wins (D11) |
| US08 | AC03 | `422` — "Task title is required." |
| US08 | AC04 | `422` — "Due date cannot be in the past." |
| US08 | AC05 | `422`, nothing saved |
| US08 | AC07 | `200`; a past due date is only checked if it changes (D12) |
| US10 | AC02 | `POST .../assignees` → `200`; the other assignees stay |
| US10 | AC03, AC10 | `DELETE .../assignees/users/{userId}` → `200`; `assignees: []` when the last one is removed |
| US10, US13 | AC01 | `GET .../eligible-assignees` → `200`; only members, and teams whose members are all in the project |
| US10 | AC04, AC05 | `422` — "The assignee must be a member of the project." |
| US10 | AC07, AC08, US13 AC09 | `200`, unchanged, no error |
| US13 | AC03 | `DELETE .../assignees/teams/{teamId}` → `200` |
| US13 | AC04 | `422` — "All team members must belong to the project." |
| US13 | AC05 | `422` — "Team not found." |
| US20 | AC01, AC04 | `GET .../tasks/{taskId}` → `200`, then `GET .../comments` |
| US20 | AC03 | `404` — "Task not found." (DoD05) |
| US21 | AC01 | `201`, with the author and `createdAt` |
| US21 | AC02, AC06 | `200`, oldest first; comments of removed members stay (D13) |
| US21 | AC03 | `422` — "Comment is required." |
| US21 | AC05 | `422` — "Comment must be at most 2000 characters." |
| US21 | AC07 | `200` with an empty list; the interface shows "No comments yet." |
| US22 | AC01 | `PATCH` → `200`, `editedAt` set |
| US22 | AC02, AC04 | Shown by the browser application before calling `DELETE` (NFR05) |
| US22 | AC03 | `204` |
| US22 | AC05 | `403` — "Only the author can change this comment." |
| US22 | AC06 | `422` — "Comment is required." |
| US22 | AC07 | `404` — "Comment not found." |
| US24 | AC01, AC02 | `GET .../tasks?priority=HIGH` → `200`, only matching tasks; the client shows only the columns of the chosen statuses |
| US24 | AC04 | `200` with an empty list; the interface shows "No tasks match the filters." |
| US24 | AC05 | `422` — "Invalid priority." / "Invalid status." |
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

- **Session cookie:** log-in sets a session cookie (`MATRIZ_SESSION`) that is `HttpOnly`, `Secure` and `SameSite=Lax`.
- **Where sessions live:** on the server, in the shared database (the SESSION entity in [database.md](database.md)). So:
  - log-out deletes the session and the cookie stops working immediately (US02 AC02);
  - any application instance can serve any request (ADR-0001).
- **CSRF:** log-in and `GET /session` return a `csrfToken` in the `Session` body. Every operation that changes data while logged in must send it in the `X-CSRF-Token` header. Log-in doesn't need it, because there is no session yet. Log-out (`DELETE /session`) does need it, because it changes data while logged in.

### Session lifecycle

Implemented in increment I1 ([implementation-plan.md](implementation-plan.md)).

1. **Log-in** (`POST /session`). The email is compared ignoring case and spaces, and the password exactly as typed.
   - On success, the server starts the session with a **new session identifier**, even if the browser sent an older cookie, so an identifier known before log-in is useless after it (session fixation). It also issues a **new CSRF token**, returned in the body.
   - The session keeps only the user's identifier and the CSRF token, never the email or the password.
   - A wrong password and an unknown email get the same `401` "Invalid email or password.", and the password is checked against a dummy hash when the email is unknown, so neither the answer nor its timing reveals which emails exist (SEC04).
2. **Using the session.** The browser sends the cookie on every request. The client keeps the CSRF token in memory only, never in `localStorage`, and sends it on every request that changes data. After a page reload, `GET /session` returns the user and the token again (US02 AC10).
3. **Identity.** Every module takes the current user from the session. No operation accepts a user identifier from the body or the URL as proof of who is asking (NFR03). The project manager and the comment author are worked out by the server.
4. **Expiry** (PD02, decided 2026-11-11). A session ends after **30 minutes without requests**, and each request renews it. Expired sessions are deleted from the database every minute.
5. **Log-out** (`DELETE /session`, with the CSRF token). The session is deleted on the server and the cookie is cleared (`Max-Age=0`). Any later request with the old cookie gets `401` "Please log in to continue." (US02 AC02).
6. **After the session ends** (log-out, expiry, or a cookie the server doesn't know), every protected request gets `401` "Please log in to continue.", **also a request that changes data**: the CSRF token ends with its session, and the user must log in again, not reload. `403` "The request could not be verified. Please reload and try again." is only for a valid session with a missing or wrong token (DoD06). The browser application shows the log-in screen with the message on any `401`.

**Project access** is checked by the Projects module (`ProjectAccess`) for every operation under `/projects/{projectId}`: `404` "Project not found." for a non-member (DoD03), `403` with the story's message for a member who tries a manager-only action. The two `404` answers differ only in `instance`, which repeats the requested path.

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
| AS09 | Statuses, priorities and roles travel as `TO_DO`, `IN_PROGRESS`, `DONE`, `LOW`, `MEDIUM`, `HIGH` and `MANAGER`, `MEMBER`. The interface shows "To do", "High", "Manager", etc. | Matches the data model. |
| AS10 | A team referenced in the body that doesn't exist returns `422`, not `404`. | `404` is kept for resources in the URL. |
| AS11 | "Today" for the due date check (D12) is the current date on the server. | Users in other time zones could see an edge case around midnight. Irrelevant for a local demo. |
| AS12 | Adding a member with no email returns `422` "Email is required.", reusing the US01 message. | Now defined in the acceptance criteria: US09 AC10, US12 AC09. |
| AS13 | Members and teams are listed alphabetically by name. | The user stories only define the order of projects (US04) and tasks (US06). |
| AS14 | ~~There is no "get one task" operation.~~ Superseded: `GET .../tasks/{taskId}` was added for the task details (US20). | Scope reconciliation SC05. |
| AS15 | Empty lists return `200` with `[]`. The messages "You are not a member of any project yet.", "No projects match your search.", "No tasks yet.", "No tasks match the filters." and "No comments yet." are shown by the interface. | They describe an empty state, not an error. |
| AS16 | The dashboard totals are computed by the server and returned with the list, so they stay complete while a search narrows it (D14). | Computing them in the client would need a second call without the search. |
| AS17 | Filters with several values use repeated query parameters (`?priority=HIGH&priority=LOW`). | Common form-style encoding (OpenAPI `style: form`, `explode: true`). |
| AS18 | Someone who isn't the author gets `403` on a comment, not `404`, because members can see every comment of the project. | Same approach as manager-only actions. |

## 6. Optional extensions

**Public registration (US01, FR01).** If brought back, it would be `POST /users` with `{name, email, password}`, public (no session or CSRF token), returning `201` with the `Session` body and the session cookie, so the new user is logged in at once. Its error mapping would be: `409` "This email is already in use." (AC02, AC03); `422` for AC04–AC07 with the messages of US01; `password` is `writeOnly` and never returned (AC09, AC10). It was removed from `openapi.yaml` so the contract describes only the MVP.

**Password recovery (F08).** No design yet: the method must be decided first, because recovery by email would conflict with C03.
