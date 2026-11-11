## Role

You are a senior full-stack developer experienced in Spring Boot, PostgreSQL and React with TypeScript. You build features as thin vertical slices, and you check that every layer follows the API contract.

## Goal

Members manage the work of each project through real API operations, from the board to the database:
- create, list, view, edit and delete the tasks of an existing project the user can access;
- change a task's status and priority, and filter the list by status and priority;
- the back end validates every field, and the interface shows the relevant error. A title made only of spaces is refused;
- the lists and counts (the board's columns and the home dashboard) are updated after each operation, without reloading the page.

## Context

- **Project:** Matriz (as in the previous prompts): a modular monolith in Java 21 and Spring Boot 4, with PostgreSQL and Flyway and a React + TypeScript browser application. The modules are `matriz.{web,accounts,projects,teams,tasks}`, with boundaries checked by `ModularityTest`. Tasks may depend on Projects and Teams (04-solution-architecture, section 3.2).
- **Read first:**
  - `AGENTS.md`;
  - `docs/implementation-plan.md` (sections 3.2, 5 and 6);
  - US05, US06, US07, US08, US20, US16 and US24, with DoD01–DoD07 and D01–D14 (especially D08, D11 and D12);
  - the Tasks operations in `openapi.yaml` and `docs/api-contract.md`;
  - TASK in `docs/database.md`;
  - wireframe screens 2 and 3 in `docs/ui/`.
- **Task fields** (decided by the user on 2026-11-11, already in US05, the contract and migration `V4_1`):
  - title: required, 3–150 characters after trimming;
  - description: up to 2000 characters;
  - status: `TODO`, `IN_PROGRESS` or `DONE`, starting as `TODO`;
  - priority: `LOW`, `MEDIUM` or `HIGH`, starting as `MEDIUM`;
  - due date: optional, and not earlier than the current day (D12).
- **What already exists:**
  - **I0:** the `task` table, with its constraints.
  - **I1:** sessions, CSRF, Problem Details, DoD07, and `ProjectAccess.requireMember`.
  - **I2:** project management. Also the home dashboard, whose task counts already come from the `task` table (`JdbcTaskCounts`), and the *project deleted* handler in Tasks. The project screen is at `#/projects/<id>`, without the board yet.
  - **Test helpers:** `ApiClient`, `TestUsers` and `ProjectFixtures`, which still write tasks straight into the database.
- **Not built yet:** every Tasks operation in the contract. Only the schema exists.

## Constraints

- **Scope:**
  - this increment covers US05 (create), US06 (board), US07 (status), US08 (edit, including priority), US20 (details), US16 (delete) and US24 (filters);
  - assignees (US10, US13) and comments (US21, US22) stay in later increments. The details and the cards show "Unassigned", and the details have no comments section yet;
  - this merges the plan's I3, I4 and I8 into one increment: record the new order and numbering in `docs/implementation-plan.md`;
  - don't add features, fields or endpoints that no story asks for (AGENTS.md §3).
- **Contract:** follow `openapi.yaml` and `docs/api-contract.md` exactly:
  - the paths, the bodies, the status codes and the Problem Details messages;
  - the field limits above;
  - repeated query parameters for filters (AS17);
  - no pagination, because the contract defines none.
- **Access:**
  - every operation under `/projects/{projectId}/tasks` uses `ProjectAccess.requireMember`: non-members get `404` "Project not found." (DoD03);
  - a task that doesn't exist in that project gets `404` "Task not found." (DoD05);
  - any member can change, move and delete any task (D08).
- **Rules:**
  - spaces are trimmed, and a blank title gets "Task title is required." (D01, D02);
  - due dates follow D12;
  - when two members change the same task, the last saved change wins (D11);
  - on the board, the cards are newest first within each column (US06).
- **Deleting:** the interface asks for confirmation before deleting, with the exact text of US16 (NFR05). Deleting a task also deletes its assignees and comments (R22).
- **Live updates:** after each operation, the board, its column counts and the details update without a page reload, and the home dashboard shows the new counts when the user opens it again.
- **Messages:** use the texts exactly as in the acceptance criteria (DoD04). Keep all interface text in English (DoD01).
- **Approval:** present a short plan before changing code, and wait for approval (AGENTS.md §4). The plan lists the files, the reasons and the checks. Any new dependency needs explicit approval (AGENTS.md §6): drag and drop uses the browser's native API, with the ← → buttons as in the wireframe.
- **Conventions:** respect the module boundaries. Database changes only through new Flyway migrations, and never edit an applied migration. Code and documents are in English.

## Task

1. **Prepare.** Read the documents listed in Context. Then present a short plan and wait for approval. The plan includes:
   - the files to change and the reasons;
   - the new order of the increments;
   - any gap or contradiction you find in the stories or the contract.
2. **Back end, Tasks module:**
   - `createTask`, `listProjectTasks` (with the `status` and `priority` filters), `getTask`, `updateTask` (fields, status and priority) and `deleteTask`;
   - validation and messages as in the contract.
3. **Browser application:**
   - **board:** on the project screen, three columns with their counts and the cards, **New task**, drag and drop and ← → to change the status, the status and priority filters with **Clear filters**, and the empty states;
   - **task details:** opened from a card, with **Edit** and **Delete task**, and the confirmation;
   - every change updates the board without a reload.
4. **Test helpers:** where an API operation now exists, prefer it to writing tasks straight into the database.
5. **Documentation:**
   - `docs/implementation-plan.md`: the new increment order, and this increment's status, results and differences;
   - README.md: what can be done now.
6. **Report** as in AGENTS.md §9. Don't commit. Wait for the user to ask for it.

## Validation

1. **Automated API tests** (real HTTP against PostgreSQL with Testcontainers):
   - every acceptance criterion of US05, US06, US07, US08, US20, US16 and US24 that the API covers, with the exact messages, including the new limits (2, 3, 150 and 151 characters in the title; 2000 and 2001 in the description);
   - a non-member gets `404` "Project not found." on every task operation;
   - a task of another project gets "Task not found.";
   - any member can change any task;
   - the dashboard counts follow creating, moving and deleting tasks;
   - invalid filter values get "Invalid status." or "Invalid priority.";
   - DoD02 (`401`), DoD06 (`403` without the CSRF token) and DoD07 (`400`) on the new operations.
2. **Browser application tests** (`npm test`):
   - the board and its counts;
   - creating a task, with an error shown;
   - moving a task with ← →;
   - the filters and their empty state;
   - editing in the details;
   - the delete confirmation;
   - the board updating without a reload.
3. **Full build:** `./mvnw verify` passes, including `ModularityTest`. If `openapi.yaml` changes, it still validates.
4. **Manual end-to-end check** with the application running (`docker compose up -d --wait`, then `SERVER_PORT=8081` if port 8080 is in use):
   1. log in, open a project and create a task;
   2. move it to Done and see the column counts change;
   3. filter the board;
   4. edit and delete a task;
   5. go back to the dashboard and see the new counts;
   6. restart the back end and see the tasks again;
   7. log in as a non-member and get "Project not found.".
5. **Report** each check and its result. Say what couldn't be checked.
