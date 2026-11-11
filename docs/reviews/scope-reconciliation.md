# Scope reconciliation (new scope vs current MVP)

- **Date:** 2026-11-10
- **Analyst:** Claude, acting as requirements analyst. The prompt is in [docs/prompts/scope-reconciliation.md](../prompts/scope-reconciliation.md).
- **Decisions:** Vitor Barros (human in the loop).
- **Status:** closed. Every difference has a decision, and the decisions are applied to the MVP documents and the Architecture Pack.

## New scope

Provided by the user. Each area lists its minimum behaviour.

| # | Area | Minimum behaviour |
|---|---|---|
| S1 | Authentication | Log in and log out, and view the authenticated user. |
| S2 | Projects | Create, list, view, edit and delete the projects the user is authorised for. |
| S3 | Tasks | Create, view, edit and delete a project's tasks, with status and priority. |
| S4 | Assignees | Assign several eligible users to a task, and remove assignments. |
| S5 | Comments | Add and list a task's comments, with author and date. |
| S6 | Search and filters | Search projects by name, and filter tasks by status and priority. |
| S7 | Home dashboard | Show the projects the user can access and the task counts per status, with navigation to the details. |
| S8 | Demo access | To simplify access, demo accounts created by a seed may be used. |
| S9 | Optional extensions | Public registration and password recovery are optional extensions. |

The user confirmed that the functional scope is complete.

## Current MVP

[README.md](../../README.md) (MVP table), the Must items in [requirements.md](../requirements.md) (FR01–FR15, FR19, FR20; NFR02–NFR05, NFR07) and the 18 stories in [user-stories.md](../user-stories.md). The Architecture Pack follows it.

## Differences

Columns: **New** is the new-scope area involved. **Affected** lists the items that would change. The **Decision** column is filled in with the user.

| ID | New | Difference | Affected | Proposed resolution | Decision |
|---|---|---|---|---|---|
| SC01 | S9 | **Public registration** is Must today (FR01, US01). In the new scope it is an optional extension. | FR01, US01 (and US02 "Depends on US01"); README MVP table; user type "Visitor … can sign up"; ASM01 (open sign-up); PRI04 risk text; `openapi.yaml` `signUp`; api-contract; data model coverage table. | FR01 → **Could**, marked "optional extension". US01 leaves the backlog and is kept in a new section "Optional extensions" of user-stories.md, unchanged, so it is ready if brought back. Remove `POST /users` from `openapi.yaml` and record it in api-contract.md as an extension. ASM01 becomes "accounts are created by the seed". || **Accepted.** |
| SC02 | S9 | **Password recovery** doesn't exist in any document. Recovery usually sends an email, which is an external integration excluded by **C03**. | requirements.md §7; constraints.md C03. | Add as future feature **F08 — Recover a forgotten password**, **Could**, "optional extension", with a note that recovery by email conflicts with C03, so the method must be decided (and C03 revisited) before it is built. No user story or endpoint now. || **Accepted.** |
| SC03 | S8 | **Seed demo accounts** are new. Today accounts only exist through sign-up. | New FR21 (Must); ASM01; PRI01 (data collected); user-stories.md (new story or setup note); README (how to log in for the demo); database.md (no new entity). | Add **FR21 — The system provides demo accounts created by a seed**, Must. Seed data is made up (no real people). Passwords are never written in the repository: they come from configuration kept out of it (AGENTS.md §7). The number of accounts and how the seed runs are decided with the stack (new pending decision **PD12**). Add a short story or setup note so it is verifiable. || **Accepted.** |
| SC04 | S1 | **View the authenticated user:** the API already has `GET /session` (returns the user), and the wireframe shows the name in the header, but no FR or acceptance criterion says so. | FR02; US02. | Extend FR02: "…log in, log out and view who is logged in". Add US02 AC10 "The logged-in user's name is shown". No API change. || **Accepted.** |
| SC05 | S3 | **View a task:** there is no task detail. The API has no `GET` for one task, and the wireframe has no task screen. Comments (S5) need one. | FR10–FR12; new story; `openapi.yaml` (new `getTask`); wireframe. | Add a task detail view: new **FR22 — view a task's details** (title, description, due date, status, priority, assignees, comments), Must, with a new story. Add `GET /projects/{projectId}/tasks/{taskId}`. Add a task detail panel to the wireframe. || **Accepted.** |
| SC06 | S3 | **Priority** is new. Tasks have no priority today. | FR10, FR11; US05, US06 (card), US08; TASK entity; `openapi.yaml` Task schemas; wireframe card and form. | Add a fixed **priority** to tasks. Proposal: three values **"Low", "Medium", "High"**, default **"Medium"**, required, editable by any member (D08). A fixed list respects C04. *Decide the values and the default.* || **Accepted:** "Low", "Medium", "High"; default "Medium". |
| SC07 | S4 | **Several assignees per task.** Today a task has at most one assignee, a person **or** a team (ASM04, FR14, R04). The new scope asks for several users and for removing each assignment. | ASM04; FR14; US10 (rewrite); D10, R10, R12; TASK (two assignee fields) → new TASK_ASSIGNEE table; `openapi.yaml` `assignTask`/`unassignTask` → add/remove one assignee; board card; wireframe selector; 04-solution-architecture (US10 sequence). | ASM04 → "a task can have zero or more assignees, all users". **Eligible** = a member of the task's project when assigned (as today, R10). Replace `PUT/DELETE …/assignee` with `POST …/assignees` and `DELETE …/assignees/{userId}`. D10 still applies: removing a project member removes only their assignments. Add a limit only if you want one (none proposed). || **Accepted**, adjusted for SC08 Option B: a task has zero or more assignees, each one an eligible user (a project member) or a team (all its members in the project when assigned, D09). ASM04 is reworded to include teams. |
| SC08 | S4 | **Teams** are not in the new scope. Today they are 4 Must FRs (FR08, FR09, FR19, FR20) and 5 stories (US11–US13, US17, US18), one of the 4 modules, and several rules (D05, D06, D09, R03, R11, R19, PRI04, PD11). The new scope assigns tasks only to users. | All of the items listed, plus ASM05, ASM13; README; ADR-0001 (four modules), architecture.md and docs/architecture/ (Teams module, "team deleted" event); database.md (TEAM, TEAM_MEMBER); `openapi.yaml` (6 team operations); wireframe ("Design (team)"). | **Option A (proposed):** move teams out of the MVP. FR08, FR09, FR19, FR20 → **Could** (F09 "Teams"); US11–US13, US17, US18 leave the backlog; the Teams module, its tables and endpoints are removed from the Architecture Pack; ADR-0001 gets an amendment (three modules), since an accepted decision changes. Frees time for comments, priority, search, filters and the dashboard. **Option B:** keep teams as extra scope beyond the minimum, adapted to several assignees (a team assignment would sit next to user assignments). More work and more rules. *Choose A or B.* || **Option B:** keep teams in the MVP, adapted to several assignees (see SC07). |
| SC09 | S2, S4 | **Project members** (FR06, US09) are not named in the new scope, but "authorised projects" (S2) and "eligible users" (S4) only make sense if someone decides who belongs to a project. | FR06, US09. | **Keep** FR06 and US09 as Must, and note in requirements.md that they support S2 and S4. || **Accepted.** |
| SC10 | S5 | **Comments** are new to the MVP. Today they are future feature F02 (Could). | F02; new FR23 and story; new COMMENT entity; new endpoints; R17 (project deletion cascade); US16 (task deletion); wireframe task detail. | Promote F02 to **FR23 — add and list a task's comments**, Must. Any project member can comment. A comment has text (1–2000 chars, proposed), author and date and time; listed oldest first. No editing or deleting comments (not asked). Deleting a task or project deletes its comments. If the author later leaves the project, the comment stays with their name. Comments belong to the Tasks module. Add `GET` and `POST /projects/{projectId}/tasks/{taskId}/comments`. | **Adjusted:** "give the options to delete and edit also. Otherwise it's too inflexible." Only the **author** can edit or delete their own comment; the project manager can't delete other people's comments. Editing uses the same limits, and an edited comment shows "(edited)". Deleting asks for confirmation (NFR05 extended to comments): `Delete this comment? This can't be undone.` Add `PATCH` and `DELETE /projects/{projectId}/tasks/{taskId}/comments/{commentId}`. |
| SC11 | S6 | **Search projects by name** is new. Not even a future feature (F03 searches *tasks* by text). | New FR24 and story (or a new AC in US04); `openapi.yaml` `listMyProjects` (query parameter); wireframe (search box). | Add **FR24 — search my projects by name**, Must: case-insensitive, matches any part of the name, only among the user's projects; no results shows a fixed message (proposed: "No projects match your search."). Implement as a `?name=` parameter on `GET /projects`. || **Accepted.** |
| SC12 | S6 | **Filter tasks by status and priority.** Today FR17 is **Should** and filters by status **and assignee**. The wireframe README says filters are out. | FR17; requirements.md §6–§7; US06 (or a new story); `openapi.yaml` `listProjectTasks` (query parameters); wireframe and ui README. | FR17 → **Must**, filtering by **status and priority** (several values allowed, combined with AND). Filtering by assignee stays **Should** as a new FR25, or is dropped. *Decide what to do with the assignee filter.* || **Accepted**, and the assignee filter stays **Should** (new FR25). |
| SC13 | S7 | **Home dashboard** with task counts per status. Today "My projects" (US04) shows only name and role. Counts could be read as "statistics", which **C02** excludes. It isn't clear whether the counts are per project or totals across all projects. | US04 (or a new story); C02 wording; `openapi.yaml` `ProjectSummary` (counts); wireframe screen 1; ui README. | Turn "My projects" into the **home dashboard**: each project shows its counts of To do, In progress and Done, and opens the project when clicked. Clarify C02: "plain counts of tasks per status are allowed; reports, charts and statistics are not". *Confirm: counts per project (proposed), totals, or both.* || **Adjusted: both.** Each project shows its own counts, and a line "All my projects" at the top shows the totals across all the user's projects. The totals always cover every project and don't change with the search (SC11). C02 is clarified: plain counts per status are allowed. |
| SC14 | — | **Current MVP items not named in the new scope:** due dates (FR10), the board (FR15), confirmations before deleting (NFR05) and the cross-cutting rules (NFR02–NFR04, NFR07). | None, unless removed. | **Keep** them. The new scope lists *minimum* behaviour, and these support it (the board shows status; NFR05 protects the deletes in S2 and S3). || **Accepted.** |
| SC15 | — | **Timeline check.** Not a scope change: a check that the timeline assumption still holds. ASM08 says "about 3 weeks for the MVP (18 user stories)", and the README says "18 stories". The reconciliation changes both the number and the size of the stories: with SC01 and SC08 Option A, 6 stories leave (US01, US11–US13, US17, US18); about 6 come in (seed accounts, task detail, comments, search, filters, dashboard); and US05, US06, US08 and US10 grow (priority, several assignees). | ASM08; README "18 stories"; user-stories.md backlog and order. | Keep ~3 weeks. In phase 2, update the story count in ASM08, the README and the backlog. If the MVP ends up clearly larger (e.g. SC08 Option B), record the timeline as a risk in ASM08. | **Accepted** as written. |

## Pending decisions touched

- **PD11** (who can see teams) disappears if SC08 Option A is chosen.
- **New PD12** (SC03): how demo accounts and their passwords are provided. Decided with the stack (PD01).
- **C03** must be revisited before password recovery (SC02) is built.
- **ADR-0001** needs an amendment if the Teams module is removed (SC08 A).

## Decision log

| ID | Decision | Applied in |
|---|---|---|
| SC01 | Accepted | requirements.md (FR01 Could, user types, §6, §7); user-stories.md (US01 moved to §6, US02 depends on US19); constraints.md (ASM01, PRI04); openapi.yaml (`POST /users` removed); api-contract.md (§1, §6); README.md |
| SC02 | Accepted | requirements.md (F08); constraints.md (C03 note) |
| SC03 | Accepted | requirements.md (FR21); user-stories.md (US19); constraints.md (SEC10, PRI05, PD12); database.md (A7, OP5); 04-solution-architecture (Accounts) |
| SC04 | Accepted | requirements.md (FR02); user-stories.md (US02 AC10); openapi.yaml (`getSession` description) |
| SC05 | Accepted | requirements.md (FR22); user-stories.md (US20); openapi.yaml (`getTask`); api-contract.md (AS14 superseded); wireframe (screen 3) |
| SC06 | Accepted: Low, Medium, High; default Medium | requirements.md (FR10, FR11); user-stories.md (US05, US06, US08); database.md (TASK.priority, R20); openapi.yaml (`TaskPriority`); domain model; wireframe |
| SC07 | Accepted, adjusted for SC08 B | constraints.md (ASM04, ASM06); requirements.md (FR14, FR15); user-stories.md (D10, US06, US09, US10, US13, US18); database.md (TASK_ASSIGNEE_USER, TASK_ASSIGNEE_TEAM, R04, R12, R19); openapi.yaml (assignee operations); domain model (I10, I13); 04-solution-architecture; wireframe |
| SC08 | Option B: keep teams | No removal. Team stories and rules adapted to several assignees (SC07). |
| SC09 | Accepted | requirements.md §6 note |
| SC10 | Adjusted: edit and delete by the author only, "(edited)" mark | requirements.md (FR23, NFR03, NFR05; F02 removed from §7); user-stories.md (US21, US22, D13); constraints.md (SEC11, PRI01); database.md (COMMENT, R21–R23); openapi.yaml (4 comment operations); domain model (Comment, I16); wireframe |
| SC11 | Accepted | requirements.md (FR24); user-stories.md (US23); openapi.yaml (`?name=`); wireframe |
| SC12 | Accepted; assignee filter stays Should (FR25) | requirements.md (FR17 Must, FR25 Should); user-stories.md (US24); openapi.yaml (`?status=`, `?priority=`); wireframe |
| SC13 | Adjusted: counts per project and totals; totals ignore the search | requirements.md (FR07); user-stories.md (US04, D14); constraints.md (C02); openapi.yaml (`ProjectDashboard`, `TaskCounts`); 04-solution-architecture (dashboard served by Tasks); wireframe |
| SC14 | Accepted: keep | No change. |
| SC15 | Accepted | constraints.md (ASM08: 23 stories, timeline risk); README.md (23 stories) |

### Changes that follow from the decisions

These weren't separate rows, but are needed to apply them consistently:
- **US18 confirmation message** changed from `…Its tasks will become unassigned…` to `Delete the team "<team name>"? It will be removed from its tasks. This can't be undone.`, because with several assignees a task doesn't always become unassigned.
- **Comment authors who left the project** (D13): Projects gets a new public operation, *Get user names*, so Tasks can show their names without a new module dependency.
- **Home dashboard served by Tasks:** `GET /projects` needs task counts, and Tasks depends on Projects, not the other way round. Serving it from Tasks keeps the dependency rules of ADR-0001 unchanged.
- **ADRs and 03-alternatives** were not changed: they record the decisions and the scope at the time, and the four modules and their dependencies still hold.

### Checks after applying

| Check | Result |
|---|---|
| `npx -y @redocly/cli@1.25.15 lint openapi.yaml` | Valid, 1 warning (the expected `localhost` server). 29 operations. |
| Markdown links and anchors | All resolve, except one link in the closed [review-01](review-01.md) (`03-alternatives.md`, missing `architecture/`), which existed before and wasn't changed. |
| Leftover references | No document still says public registration is Must, that a task has one assignee, or that removing a member or a team unassigns tasks. |
| Traceability | Every Must FR and NFR has at least one story; the traceability table and the story headers match; the backlog order matches each story. |
| Counts | 23 MVP stories (README, ASM08, backlog), 29 operations (openapi.yaml, api-contract.md, architecture.md). |
| Wireframe | Tested in a browser (Playwright): search, totals, filters, assignees, comments and confirmations work; no horizontal scroll at 400 px; no script errors. Published as version 5. |
