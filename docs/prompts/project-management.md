## Role

You are a senior full-stack developer experienced in Spring Boot, PostgreSQL and React with TypeScript. You build features as thin vertical slices, and you check that every layer follows the API contract.

## Goal

Deliver the first complete path between the interface, the API and the database: project management with real persistence, on top of the working authentication.
- Users can create, list, view, edit and delete projects, with the requests, responses and status codes of the contract.
- Each project belongs to the authenticated user who created it. The list and every operation only reach projects the user can access.
- **First increment to finish:** log in, create a project, see it in the list, and see it again after reloading the page and restarting the back end.

## Context

- **Project:** Matriz, a simplified Jira-like project and task management application. It is a modular monolith in Java 21 and Spring Boot 4, with PostgreSQL and Flyway and a React + TypeScript browser application served from the same JAR. The modules are `matriz.{web,accounts,projects,teams,tasks}`. Types in the top-level package of a module are its public interface, and `internal` sub-packages are private. `ModularityTest` checks these boundaries, and the allowed dependencies are in `docs/architecture/04-solution-architecture.md`, section 3.2.
- **Read first:**
  - `AGENTS.md`;
  - `docs/implementation-plan.md` (increments, access matrix section 3.1, operations per increment);
  - US03, US04, US14, US15 and US23 in `docs/user-stories.md`, with the cross-cutting criteria DoD01–DoD07 and decisions D01–D14;
  - `openapi.yaml` and `docs/api-contract.md` (the Projects operations and section 4, Authentication);
  - `docs/database.md` (PROJECT, PROJECT_MEMBER, rule R17).
- **What already exists:**
  - **I0:** the whole schema, including `project`, `project_member` and the Tasks tables, which reference `project` with `ON DELETE RESTRICT` (PD04). The demo seed creates Ana, Rui and Sara, and the projects *Website* (Ana manager, Rui member, Sara not a member) and *App* (Rui manager, Ana member).
  - **I1:**
    - log-in, `GET /api/session` and log-out with server-side sessions and CSRF;
    - errors as Problem Details;
    - the browser application's log-in screen, header and redirect on `401`;
    - `matriz.projects.ProjectAccess`, with `requireMember` (`404` "Project not found.") and `requireManager` (`403` with the message of `ManagerOnlyAction`);
    - modules take the current user from `Authentication`, as described in `matriz/web/package-info.java`.
  - The home screen is a placeholder, waiting for the dashboard (US04).

## Constraints

- **Scope:**
  - this increment covers US03 (create), US04 (home dashboard with the list), US14 (edit), US15 (delete) and US23 (search by name);
  - tasks, members (US09) and teams stay in later increments;
  - this changes the order of the plan: record the new order in `docs/implementation-plan.md` (US14 and US15 move from I6, US23 from I8, and US05 and US06 move to the next increment);
  - don't add features, fields or endpoints that no story asks for (AGENTS.md §3).
- **Contract:** follow `openapi.yaml` and `docs/api-contract.md` exactly:
  - the paths, the bodies, the status codes and the Problem Details messages;
  - the name is 1–100 characters and the description up to 2000, after trimming (D01, D02). These are the limits of the adopted contract, not the Aula 02 example (3–100 and 1000);
  - the contract defines no pagination, so don't add any;
  - `GET /projects` returns `{totals, projects}` with `taskCounts` per status, as the contract says. The counts are zero until tasks exist.
- **Identity and access:**
  - the creator becomes the project's manager and a member (D07, R08). The manager is always taken from the session, never from the request;
  - the list only shows the user's projects;
  - every operation under `/projects/{projectId}` uses `ProjectAccess`: non-members get `404` "Project not found." (DoD03), and members who aren't the manager get `403` with the story's message on edit and delete (US14 AC04, US15 AC04).
- **Search:** follow US23 and D14. The `name` parameter narrows the list, and the totals still cover all of the user's projects.
- **Deleting:**
  - the interface asks for confirmation before deleting, with the exact text of US15 (NFR05);
  - the API applies R17 and FR05: the project, its memberships, and its tasks with their assignees and comments are deleted in one transaction, so no orphan rows remain;
  - implement the *project deleted* event with the Tasks module's handler now (04-solution-architecture, section 3.2, rule 4), even though no tasks can be created yet.
- **Messages:** use the texts exactly as in the acceptance criteria (DoD04). Keep all interface text in English (DoD01).
- **Approval:** present a short plan before changing code, and wait for approval (AGENTS.md §4). The plan lists the files, the reasons and the checks. A new dependency, such as a router library, needs explicit approval (AGENTS.md §6).
- **Conventions:** respect the module boundaries. Database changes only through new Flyway migrations, and never edit an applied migration. Code and documents are in English.

## Task

1. **Prepare.** Read the documents listed in Context. Then present a short plan and wait for approval. The plan includes:
   - the files to change and the reasons;
   - the new order of the increments;
   - any gap or contradiction you find in the stories or the contract.
2. **Back end, Projects module:**
   - `createProject`, `getProject`, `updateProject` and `deleteProject`, with validation and messages as in the contract;
   - the creator becomes the manager and a member;
   - deleting publishes *project deleted* inside the transaction.
3. **Back end, home dashboard:** `listMyProjects` (`GET /projects?name=`), served by the Tasks module as confirmed in the plan (Q3):
   - only the user's projects, with their role, sorted by name ignoring case;
   - `taskCounts` per project and `totals` across all of them (D14).

   Add the Tasks handler of *project deleted*.
4. **Browser application:**
   - the home dashboard with the project list, the counts, the totals, the search box and the empty-state messages;
   - **New project**;
   - the project screen with edit and delete, both for the manager only. Delete asks for confirmation first.

   After a reload the user stays where they were, or returns to the dashboard.
5. **Documentation:**
   - `docs/implementation-plan.md`: the new increment order, and this increment's status, results and differences;
   - README.md: what can be done now.
6. **Report** as in AGENTS.md §9. Don't commit. Wait for the user to ask for it.

## Validation

1. **Automated API tests** (real HTTP against PostgreSQL with Testcontainers, with the demo users):
   - every acceptance criterion of US03, US04, US14, US15 and US23 that the API covers, with the exact messages;
   - the creator becomes the manager, and no user identifier is accepted from the request;
   - Sara gets `404` "Project not found." on *Website* for get, edit and delete, and *Website* isn't in her list;
   - Rui gets `403` on editing and deleting *Website*, and his list shows his role in each project;
   - the totals don't change with a search (D14);
   - deleting a project leaves no rows in `project_member` or in the Tasks tables for that project;
   - DoD02 (`401`), DoD06 (`403` without the CSRF token) and DoD07 (`400`) on the new operations.
2. **Browser application tests** (`npm test`): the dashboard, creating, the search and its empty state, editing, and the delete confirmation.
3. **Full build:** `./mvnw verify` passes, including `ModularityTest`. If `openapi.yaml` changes, it still validates.
4. **Manual end-to-end check** with the application running (`SERVER_PORT=8081` if port 8080 is in use):
   1. log in, create a project and see it in the list;
   2. reload the page and see it again;
   3. restart the back end, log in again, and see it again;
   4. log in as another user and check that they don't see it.
5. **Report** each check and its result. Say what couldn't be checked.
