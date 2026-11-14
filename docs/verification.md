# Matriz — Verification of the MVP

This record shows, scenario by scenario, that the application meets its requirements and keeps its data. Each row gives the expected result (from the acceptance criteria), the result actually observed, the evidence, and whether the check was done **by hand** in a real browser against the running application (*Manual*) or by a test (*Automated*).

- **Date:** 2026-11-12.
- **Prompt:** [docs/prompts/verification.md](prompts/verification.md).
- **Result:** 63 checks, **63 Pass, 0 Fail, 0 Not verified**. No bug was found, so no fix was needed. What couldn't be run is listed in [section 6](#6-not-run-and-limitations).

## 1. Environment

| Item | Version or value |
|---|---|
| Machine | macOS 26.6.2 |
| Java | OpenJDK 21.0.9 |
| Database | PostgreSQL 17.10, in Docker (`compose.yaml`); the tests use their own throw-away PostgreSQL 17.10 (Testcontainers) |
| Browser application build | Node v24.15.0 (downloaded by Maven) |
| Browser for the manual checks | Chromium, driven by Playwright 1.47.2 (installed outside the project, not a dependency) |
| Application | `target/matriz-0.1.0-SNAPSHOT.jar`, on `http://localhost:8081` (port 8080 is used by another program on this machine) |
| Configuration | The local `.env`, with `MATRIZ_COOKIE_SECURE=false` for `http://localhost` (api-contract.md, section 4) |
| Accounts | The demo accounts (made up, PRI05). In *Website*, Ana is the manager, Rui a member and Sara not a member; in *App*, Rui is the manager. |

## 2. Commands run

| Command | Result |
|---|---|
| `./mvnw -o test -Dtest=VerificationScenariosTest` | The three new tests: 3 run, 0 failures. |
| `./mvnw -o verify` | **BUILD SUCCESS** in 1 min 53 s: **174** back-end tests (14 classes) and **46** browser application tests (10 files), 0 failures, with `ModularityTest`. |
| `docker compose up -d --wait`, then `SERVER_PORT=8081 java -jar target/matriz-0.1.0-SNAPSHOT.jar` | The application against the local database, for the manual checks. |
| `docker compose down`, then `docker compose up -d --wait` | The database restart in case 03. The data volume is kept. |
| A fingerprint query on every table: the row count and an md5 of the rows' text | Shows whether a table's content changed, without printing any value (section 4). |

**The three new automated tests** are in [`src/test/java/matriz/VerificationScenariosTest.java`](../src/test/java/matriz/VerificationScenariosTest.java):

| Test | Case | What it checks |
|---|---|---|
| `case04_aTitleOfOnlySpacesIsRefusedAndNothingIsStored` | 04 | A task title of only spaces gets `422` "Task title is required.", and the `task` table has no row for the project. |
| `case08_changingIdentifiersInTheRequestGetsNoAccess` | 08 | A non-member uses the real identifiers, a member of another project puts this project's task under their project's address, and a member who isn't the author targets someone else's comment. They get `404` "Project not found.", `404` "Task not found." and `403` "Only the author can change this comment.", and the task and comment rows are unchanged. |
| `case03_aCreatedTaskIsStoredAndListed` | 03 | A created task's row holds what was sent (title trimmed, description, due date, priority, status `TODO`, creation time, no assignees), and the board lists it first. |

## 3. Results by case

### Case 01 — Access to the application

| # | Step | Expected | Observed | Evidence | Kind | Result |
|---|---|---|---|---|---|---|
| 1.1 | A visitor opens the application | The log-in screen, with "Please log in to continue." (DoD02) | Log-in screen, "Please log in to continue." | Browser | Manual | Pass |
| 1.2 | `GET /api/projects` without a session | `401` "Please log in to continue." (DoD02) | `401`, "Please log in to continue." | Request from the page | Manual | Pass |
| 1.3 | Log in with a wrong password | "Invalid email or password." (US02 AC03) | "Invalid email or password." | [01-login-refused.png](verification/01-login-refused.png) | Manual | Pass |
| 1.4 | Log in with an unknown email | The same message as 1.3 (US02 AC04, AC09, SEC04) | "Invalid email or password." | Browser | Manual | Pass |
| 1.5 | Log in with empty fields | "Email and password are required." (US02 AC05) | "Email and password are required." | Browser | Manual | Pass |
| 1.6 | Log in as Ana | The home dashboard, with "Ana Silva" in the header (US02 AC01, AC10) | "My projects", header "Ana Silva" | Browser | Manual | Pass |
| 1.7 | Log-out, session fixation, CSRF, cookie attributes, logs | Old session refused, new session id at log-in, `403` without the token, `HttpOnly` / `Secure` / `SameSite=Lax`, no secret in the logs | All 14 tests pass | `SessionApiTest` | Automated | Pass |

### Case 02 — Project management

| # | Step | Expected | Observed | Evidence | Kind | Result |
|---|---|---|---|---|---|---|
| 2.1 | Ana creates "Launch plan …" | Listed, with the role Manager (US03 AC01) | Listed, role "Manager" | Browser | Manual | Pass |
| 2.2 | Ana searches part of its name | Only matching projects; the totals unchanged (US23 AC01, D14) | 1 row; totals unchanged | [02-dashboard-search.png](verification/02-dashboard-search.png) | Manual | Pass |
| 2.3 | Ana renames it | The new name is shown (US14 AC01) | Heading "Launch plan … v2" | Browser | Manual | Pass |
| 2.4 | Ana deletes it | The confirmation `Delete "<name>" and all its tasks? This can't be undone.`, then gone (US15 AC01, AC02) | That exact text; not listed afterwards | [02-delete-confirmation.png](verification/02-delete-confirmation.png) | Manual | Pass |
| 2.5 | Rui (member) edits *Website* through the API | `403` "Only the project manager can edit the project." (US14 AC04) | `403`, that message | Request from Rui's page | Manual | Pass |
| 2.6 | Rui deletes *Website* through the API | `403` "Only the project manager can delete the project." (US15 AC04) | `403`, that message | Request from Rui's page | Manual | Pass |
| 2.7 | Rui opens *Website* | No **Edit** or **Delete project** | 0 and 0 buttons | Browser | Manual | Pass |
| 2.8 | Sara (not a member) opens *Website* | "Project not found." (DoD03) | "Project not found." | [08-sara-project-not-found.png](verification/08-sara-project-not-found.png) | Manual | Pass |
| 2.9 | Every US03, US04, US14, US15 and US23 criterion the API covers | As in the stories | 24 + 16 + 5 tests pass | `ProjectsApiTest`, `DashboardApiTest`, `ProjectAccessTest` | Automated | Pass |

### Case 03 — Task persistence

| # | Step | Expected | Observed | Evidence | Kind | Result |
|---|---|---|---|---|---|---|
| 3.1 | Ana creates a task on the board | It appears at once, without a reload (US05 AC01, US06) | Board "To do 1 · In progress 0 · Done 0" right after saving | Browser | Manual | Pass |
| 3.2 | Reload the page | Still there | Still there | [03-task-on-board.png](verification/03-task-on-board.png) | Manual | Pass |
| 3.3 | Restart the back end, log in again | The task, its status, assignees and comment are unchanged | Title, status Done, assignees "QA … (team), Rui Costa", the comment: all there | [03-after-backend-restart.png](verification/03-after-backend-restart.png); fingerprints (section 4) | Manual | Pass |
| 3.4 | Restart PostgreSQL (`down`, `up`), restart the back end | As in 3.3 | All there | [03-after-database-restart.png](verification/03-after-database-restart.png); fingerprints | Manual | Pass |
| 3.5 | Start the application again (the seed runs) | The seed changes and deletes nothing (FR21, US19) | Every table's fingerprint identical before and after | Fingerprints (section 4) | Manual | Pass |
| 3.6 | Create a task and read its row | The row holds what was sent; the board lists it | Pass | `VerificationScenariosTest.case03_aCreatedTaskIsStoredAndListed` | Automated | Pass |
| 3.7 | Run the seed twice | No row added or changed | Pass | `PersistenceSetupTest.seedRunTwiceDoesNotChangeTheData` | Automated | Pass |

### Case 04 — Data validation

| # | Step | Expected | Observed | Evidence | Kind | Result |
|---|---|---|---|---|---|---|
| 4.1 | Task title of only spaces | "Task title is required." (US05 AC03, D01); the typed text stays | That message; text kept | Browser | Manual | Pass |
| 4.2 | Task title of 2 characters | "Task title must be at least 3 characters." (US05 AC08) | That message; text kept | Browser | Manual | Pass |
| 4.3 | Task title of 151 characters | "Task title must be at most 150 characters." (US05 AC08) | That message; text kept | Browser | Manual | Pass |
| 4.4 | Due date in the past | "Due date cannot be in the past." (US05 AC04) | That message; the form stays open | [04-validation-kept-input.png](verification/04-validation-kept-input.png) | Manual | Pass |
| 4.5 | Priority "Urgent" through the API | `422` "Invalid priority." (US05 AC10) | `422`, that message | Request from the page | Manual | Pass |
| 4.6 | An unknown field (`status`) in a new task | `400` "The request body is not valid." (DoD07) | `400`, that message | Request from the page | Manual | Pass |
| 4.7 | A project name of only spaces | `422` "Project name is required." (US03 AC03) | `422`, that message | Request from the page | Manual | Pass |
| 4.8 | A title of only spaces, and the table afterwards | `422`, nothing stored | Pass | `VerificationScenariosTest.case04_aTitleOfOnlySpacesIsRefusedAndNothingIsStored` | Automated | Pass |
| 4.9 | Every limit, date, status, priority and body rule | As in US03, US05, US07, US08, US14, DoD07 | Pass | `TasksApiTest` (49), `ProjectsApiTest` (24), `PersistenceSetupTest.theTaskTableEnforcesTheTaskLimits` | Automated | Pass |

### Case 05 — Status and priority

| # | Step | Expected | Observed | Evidence | Kind | Result |
|---|---|---|---|---|---|---|
| 5.1 | Move the task with → using the keyboard | The columns' counts follow (US07 AC01) | "To do 0 · In progress 1 · Done 0" | Browser | Manual | Pass |
| 5.2 | Filter by priority Low | "No tasks match the filters." (US24 AC04) | That message | Browser | Manual | Pass |
| 5.3 | Filter by priority High | Only the High task, with the counts of what is shown (US24 AC01) | "To do 0 · In progress 1 · Done 0" | [05-board-filtered.png](verification/05-board-filtered.png) | Manual | Pass |
| 5.4 | In the details: status Done, priority Low | Saved and shown (US07, US08) | Status DONE, priority "Low" | Browser | Manual | Pass |
| 5.5 | Open the home dashboard | The project's counts follow (US04 AC07, AC09) | Row "Verification … Manager 0 0 1" | Browser | Manual | Pass |
| 5.6 | Counts after creating, moving and deleting; the board's update without a reload | As in US04, US06, US07 | Pass | `TasksApiTest.theDashboardCountsFollowCreatingMovingAndDeleting`, `Board.test.tsx` | Automated | Pass |

### Case 06 — Assignees

| # | Step | Expected | Observed | Evidence | Kind | Result |
|---|---|---|---|---|---|---|
| 6.1 | Ana assigns Rui in the details | Rui is an assignee (US10 AC01) | "Rui Costa" listed | Browser | Manual | Pass |
| 6.2 | Assign Rui again through the API | `200`, no duplicate (US10 AC07) | `200`, assignees "Rui Costa" once; **1** row in `task_assignee_user` | Request from the page; database query | Manual | Pass |
| 6.3 | Ana assigns the team "QA …" (Ana and Rui, both in the project) | The team is an assignee (US13 AC01, AC02) | "QA … (team), Rui Costa"; **1** row in `task_assignee_team` | [06-07-task-assignees-comments.png](verification/06-07-task-assignees-comments.png); database query | Manual | Pass |
| 6.4 | The choices for another assignee | People still unassigned; a team with a member outside the project shown disabled, with the rule (US13 AC04) | "Ana Silva"; "JBA (team) (not all members are in the project)" disabled; "All team members must belong to the project." | Browser | Manual | Pass |
| 6.5 | Removing, the last one, twice, other fields unchanged, refusals | As in US10 and US13 | 13 tests pass | `AssigneesApiTest`, `Assignees.test.tsx` | Automated | Pass |

### Case 07 — Comments

| # | Step | Expected | Observed | Evidence | Kind | Result |
|---|---|---|---|---|---|---|
| 7.1 | Ana comments and edits her comment | The text, her name, the date and time, "(edited)" (US21 AC01, US22 AC01) | "Ana Silva · 2026-11-12 22:31 (edited)", the edited text | [06-07-task-assignees-comments.png](verification/06-07-task-assignees-comments.png) | Manual | Pass |
| 7.2 | The stored row | The author is the logged-in user; creation and edit times are set | Text as typed; author `ana@example.com`; `created_at` and `edited_at` set | Database query | Manual | Pass |
| 7.3 | Rui opens the task | He sees Ana's comment, without Edit or Delete (SEC11) | Author "Ana Silva"; 0 buttons | Browser | Manual | Pass |
| 7.4 | Order, limits, author-only changes, deletion, the author leaving | As in US21, US22, D13 | 13 tests pass | `CommentsApiTest`, `Comments.test.tsx` | Automated | Pass |

### Case 08 — Authorization in the API

All the requests below were sent from the user's own page, with their session and CSRF token, and with the identifiers changed by hand.

| # | Step | Expected | Observed | Evidence | Kind | Result |
|---|---|---|---|---|---|---|
| 8.1 | Sara reads the task with its real project and task ids | `404` "Project not found.", no task data (DoD03, SEC03) | `404`, that message; the title isn't in the answer | Request from Sara's page | Manual | Pass |
| 8.2 | Sara edits the task, and comments on it | `404` "Project not found." | `404` and `404`, that message | Request from Sara's page | Manual | Pass |
| 8.3 | Rui sends the task's id under *App*'s address (he manages *App*) | `404` "Task not found." (DoD05) | `404`, that message | Request from Rui's page | Manual | Pass |
| 8.4 | Rui edits Ana's comment | `403` "Only the author can change this comment." (US22 AC05) | `403`, that message | Request from Rui's page | Manual | Pass |
| 8.5 | Rui (member, not manager) adds a member | `403` "Only the project manager can manage members." (US09 AC07) | `403`, that message | Request from Rui's page | Manual | Pass |
| 8.6 | Ana creates a project with Sara's id as `managerId` | Refused; the manager always comes from the session (NFR03) | `400` "The request body is not valid."; no project "Forged" in the database | Request from the page; database query | Manual | Pass |
| 8.7 | The task after 8.1–8.4 | Unchanged | Same title, status, priority and project | Database query | Manual | Pass |
| 8.8 | The identifier swaps of 8.1–8.4, and the rows afterwards | As above; rows unchanged | Pass | `VerificationScenariosTest.case08_changingIdentifiersInTheRequestGetsNoAccess` | Automated | Pass |
| 8.9 | The access check for every role; a member removed while being assigned (PD05) | As in DoD03, NFR03; no assignment survives a removal | Pass | `ProjectAccessTest`, `MembersApiTest` (14, including the concurrency test) | Automated | Pass |

### Case 09 — Deleting and errors

| # | Step | Expected | Observed | Evidence | Kind | Result |
|---|---|---|---|---|---|---|
| 9.1 | A refused new task (due date in the past) | The API's message; no success message; the typed data stays | The message; the form still open with the title and the date filled | [04-validation-kept-input.png](verification/04-validation-kept-input.png) | Manual | Pass |
| 9.2 | Ana deletes the team "QA …" | The confirmation of US18 AC01 | `Delete the team "QA …"? It will be removed from its tasks. This can't be undone.` | Browser | Manual | Pass |
| 9.3 | The task afterwards | The team leaves its tasks; the other assignees stay (FR20, US18 AC02) | Assignees "Rui Costa" only | Browser | Manual | Pass |
| 9.4 | Ana deletes the project "Verification …" | The confirmation of US15 AC01, then gone | That exact text; not listed afterwards | [09-delete-project-confirmation.png](verification/09-delete-project-confirmation.png) | Manual | Pass |
| 9.5 | The project's dependent rows | None left (FR05, R17, R22); the accounts stay | Before: 1 project, 2 members, 1 task, 1 person and 1 team assigned, 1 comment. After: **0** of each; 3 accounts | Database query | Manual | Pass |
| 9.6 | Deleting projects, tasks and teams with dependent data | Everything dependent deleted in one transaction; teams and accounts kept | Pass | `ProjectsApiTest.deletingAProjectDeletesEverythingThatBelongsToIt`, `TeamsApiTest.deletingATeamRemovesItFromItsTasksOnly`, `TasksApiTest.deleteATaskWithItsAssigneesAndComments` | Automated | Pass |
| 9.7 | Refusals on forms and double clicks | The message shown, the typed data kept, one request only | Pass | `MembersPage.test.tsx`, `TeamsPage.test.tsx`, `Comments.test.tsx`, `Dashboard.test.tsx` | Automated | Pass |

## 4. Data persistence: table fingerprints

The query counts the rows of every table and takes an md5 of their text, so a change anywhere shows without printing any value. The first 8 characters of each md5 are shown.

| Table | Start | Before the back-end restart | After it (seed ran) | After the PostgreSQL restart | End (after clean-up) |
|---|---|---|---|---|---|
| users | 3 · a39a9a2d | 3 · a39a9a2d | 3 · a39a9a2d | 3 · a39a9a2d | 3 · a39a9a2d |
| project | 3 · 869ea73a | 4 · 9b48689c | 4 · 9b48689c | 4 · 9b48689c | 3 · 869ea73a |
| project_member | 7 · 0ddf0a25 | 9 · 69523cf5 | 9 · 69523cf5 | 9 · 69523cf5 | 7 · 0ddf0a25 |
| team | 1 · daf9b0ac | 2 · ecf52e15 | 2 · ecf52e15 | 2 · ecf52e15 | 1 · daf9b0ac |
| team_member | 3 · 92f41d13 | 5 · 80e3f867 | 5 · 80e3f867 | 5 · 80e3f867 | 3 · 92f41d13 |
| task | 4 · efaa4beb | 5 · 4f911a71 | 5 · 4f911a71 | 5 · 4f911a71 | 4 · efaa4beb |
| task_assignee_user | 2 · 70c610ba | 3 · 01bcf545 | 3 · 01bcf545 | 3 · 01bcf545 | 2 · 70c610ba |
| task_assignee_team | 1 · 43d8a5be | 2 · 9064c568 | 2 · 9064c568 | 2 · 9064c568 | 1 · 43d8a5be |
| task_comment | 0 · d41d8cd9 | 1 · 298b93ad | 1 · 298b93ad | 1 · 298b93ad | 0 · d41d8cd9 |

- **Both restarts kept every row unchanged,** and starting the application again (with the demo seed on) changed nothing.
- **The end state equals the start state,** so the clean-up left nothing behind and deleted nothing else.

## 5. Data created and deleted for the manual checks

| Created | Deleted |
|---|---|
| Project "Launch plan … v2" | In case 02, through the interface |
| Project "Verification …", with Rui as a member, the task "Check the contact form …", its two assignees and one comment | In case 09, through the interface (the project's deletion removed the rest) |
| Team "QA …", with Ana and Rui | In case 09, through the interface |

The project "Forged" of 8.6 was never created. The fingerprints in section 4 confirm the clean-up. The other data in the local database (the demo projects, the project "Cool Stuff", the team "JBA" and their tasks) belongs to the user and wasn't touched.

## 6. Not run, and limitations

These items weren't run, and aren't counted in the totals above:
- **Safari itself.** The log-in fix for Safari (`MATRIZ_COOKIE_SECURE=false`) was checked earlier in WebKit, Safari's engine, on 2026-11-11 (implementation-plan.md, I4–I6 browser application). These checks ran in Chromium only.
- **A screen reader.** Labels, keyboard use (log-in and moving a task) and contrast were checked, but no screen reader was used.
- **Two people at the same time in two browsers.** The concurrency rule of PD05 is checked by an automated test (`MembersApiTest.anAssignmentAndARemovalAtTheSameTimeLeaveNoAssignmentBehind`), not by hand.
- **Session expiry after 30 minutes** (PD02). The automated test checks the stored timeout; nobody waited 30 minutes.
- **A deployed server with HTTPS.** Every check ran on `localhost`.

**Limitations of the method:**
- The manual checks were driven by a Playwright script, which clicks and types in the browser like a user and reads the screen. The script is in the session's scratchpad, not in the repository.
- The requests in case 08 were sent from the users' own pages, with their real sessions, to show that changing identifiers doesn't help. They don't cover an attacker with another user's session.

## 7. Known problems

None found in this verification. The delivery review that followed found two more, both fixed with regression tests (`ReviewRegressionTest`, which fail without the fixes), and recorded in implementation-plan.md, "Review for the delivery":
- **R1:** a body that isn't JSON (e.g. `text/plain`) got `415` with a technical message; it now gets DoD07's `400` "The request body is not valid.".
- **R2:** a creation racing with a deletion ended in a `500`; the deletion now locks first, and a late creation gets the deleted resource's `404` (api-contract.md, AS24).

After these fixes, `./mvnw verify` runs **179** back-end tests and **46** browser application tests, all passing.

## 8. Examples of AI use

The project was built with an AI assistant (Claude Code), with every decision taken by the developer. Three examples of how a request was handled, decided and checked:

| | Aim of the request | Developer's decision | How it was checked |
|---|---|---|---|
| **1. Safari log-in** | The developer reported that in Safari the log-in accepted the credentials but didn't go further. The assistant was asked to make it work. It diagnosed that the session cookie is `Secure` (SEC06), and that Safari, unlike Chrome and Firefox, keeps no `Secure` cookie on `http://localhost`. It proposed making the flag configurable, `Secure` by default. | The developer approved changing a security setting, limited to `http://localhost` through `MATRIZ_COOKIE_SECURE=false` in the local `.env`, with the cookie still `Secure` by default and on any HTTPS server. | Reproduced in WebKit, Safari's engine, before the fix: log-in answered `200`, no cookie was kept, and the screen went back to log-in. With the flag off: the cookie was kept and "My projects" opened. `SessionApiTest` keeps testing the `Secure` cookie, and `CookieDefaultsTest` checks the default stays `true`. |
| **2. Assigning people while creating a task (suggestion rejected)** | The developer asked whether people and teams could be assigned in the **New task** form. The assistant answered that US05 says a new task starts unassigned, and offered three options: assign from the interface right after creating, change the contract, or leave it as it is. It recommended the first. | The developer **rejected** the recommendation and chose to leave it as it is: tasks are created unassigned and assigned in their details, as US05 says. Nothing was changed. | The choice is recorded in implementation-plan.md (I4–I6 browser application, differences). The behaviour is covered by `TasksApiTest.createATaskWithEveryField` (a new task has no assignees) and case 06 of this record. |
| **3. Assigning a member who is being removed (PD05)** | The developer asked to implement members and assignees. The assistant pointed out the pending decision PD05 (an assignment saved just after the person left the project, US10 AC05) and explained the options. It then wrote a test that forces both operations to run at the same time. | The developer chose to lock the membership row (option a), after asking for an explanation of the problem. | The test `MembersApiTest.anAssignmentAndARemovalAtTheSameTimeLeaveNoAssignmentBehind` passes. To show that it really tests the lock, the `FOR SHARE` was removed for one run: the test then **failed** ("expected: 0 but was: 1", an assignment survived), and passed again once the lock was back. |

## 9. Running from a clean copy

The README's steps were followed word for word in a fresh clone, on 2026-11-12, with a new, empty database (the developer's PostgreSQL was stopped meanwhile, then started again with its data intact).

| Step (README) | Command | Observed |
|---|---|---|
| 1. Get the code | `git clone … matriz-clean` | 191 files, at commit `953e8d5` |
| 2. Configure | `cp .env.example .env` (the example values kept) | Done |
| 3. Start the database | `docker compose up -d --wait` | A new container and volume, healthy |
| 4. Build and run the tests | `./mvnw verify` | **BUILD SUCCESS** in 112 s: 179 back-end and 46 browser application tests pass |
| 5. Start the application | `SERVER_PORT=8081 java -jar target/matriz-0.1.0-SNAPSHOT.jar` (8080 is taken on this machine, as the README allows) | The 6 Flyway migrations created the schema, up to version 4.1; no error in the log |
| 6. Open it | `/actuator/health`, `/`, log-in as Ana with the example password | `{"status":"UP"}`; the start page; `200` |
| Demo data | Log in as Rui in the browser | *App* (Manager) and *Website* (Member), created by the seed: [10-clean-copy-dashboard.png](verification/10-clean-copy-dashboard.png) |

Afterwards, the clean copy's container and volume were deleted (`docker compose down -v` in the clone only).

## 10. Unit tests of the task title rule

On 2026-11-14 the rule for a task's title was tested at unit level, from the specification and not from the code: required, between 3 and 150 characters, counted after leading and trailing spaces are removed ([user-stories.md](user-stories.md), US05 AC03, AC08, D01, D02; [test-plan.md](test-plan.md), TP03, TP04). The production code was read only to find the unit to call, `TaskFields.title`. The expected messages are written in the test as the stories give them.

**Test class:** `src/test/java/matriz/tasks/internal/TaskTitleRuleTest.java`. It runs with no Spring context, database or HTTP.

| What is checked | Values | Expected (specification) | Observed |
|---|---|---|---|
| Valid titles are accepted unchanged | 3, 4, 75, 149, 150 characters | Accepted | Pass |
| Spaces around the title are removed, spaces inside are kept | "  Review copy  "; "Design  home page" | "Review copy"; unchanged | Pass |
| An empty title, or one with only spaces, is required | "", " ", "   " | "Task title is required." | Pass |
| Too short | 1, 2 characters | "Task title must be at least 3 characters." | Pass |
| Too long | 151, 300 characters | "Task title must be at most 150 characters." | Pass |
| The minimum counts after removing spaces | "  ab  " | "Task title must be at least 3 characters." | Pass |
| The maximum counts after removing spaces | 150 characters with two spaces on each side | Accepted, without the spaces | Pass |

**Commands and results:**

| Command | Result |
|---|---|
| `./mvnw test -Dtest=TaskTitleRuleTest -Dskip.npm -Dskip.installnodenpm` | **16 tests, 16 passed, 0 failed** |
| The same, with the rule's maximum temporarily changed from 150 to 149 | **2 failed**, as expected: the 150-character title and the 150-character title with spaces around it were refused. The change was then reverted, and `git diff` shows no change under `src/main`. |
| `./mvnw verify` | **BUILD SUCCESS**: 195 back-end tests (179 before, plus these 16), 0 failed, together with the browser application tests and `ModularityTest` |

**Tests corrected:** one. The first version also treated a title made of a tab (`"\t"`) as missing. It passed, but the specification doesn't support it: D01 speaks of "spaces" and doesn't say whether a tab counts as one. The case was removed instead of being kept as a guess. The question is left open for the specification.

**Defects found:** none. The rule meets US05, D01 and D02 for every value tested, and the rule was not changed.

## 11. Integration tests: creating and reading a task

On 2026-11-14 the API and the real persistence were tested together for creating and reading a task. The tests use real HTTP requests, with a session and verification token, against a PostgreSQL database in a container (`TestcontainersConfiguration`).
- **Status codes and fields:** from the API contract ([openapi.yaml](../openapi.yaml)).
- **Messages and rules:** from the user stories (US05, US20, DoD03, DoD05).

The production code was read only to learn how to call the application.

**Test class:** `src/test/java/matriz/TaskApiPersistenceTest.java`.

**Isolation:**
- Each test creates its own two users and project, with unique names.
- When it ends, it deletes the project (and with it its tasks), logs both users out and deletes them.
- It then checks that the `project`, `task` and `users` tables have the same number of rows as before it started.

| Test | Request | Expected (contract and stories) | Observed |
|---|---|---|---|
| A member creates a valid task and reads it back | `POST /api/projects/{id}/tasks` with every field, then `GET …/tasks/{taskId}` | `201`, then `200`; both bodies have the same id, project, title, description, due date, status `TODO`, priority `HIGH`, no assignees and `createdAt`; the `task` row has the same values, and no assignee rows exist | Pass |
| Someone who isn't a member can't read the task | `GET …/tasks/{taskId}` as a non-member | `404`, `application/problem+json`, "Project not found.", no task field in the answer | Pass |
| A title that is too short is refused and nothing is stored | `POST …/tasks` with the title "ab" | `422`, `application/problem+json`, "Task title must be at least 3 characters.", the project has no task | Pass |
| A task that was deleted is not found | `DELETE …/tasks/{taskId}` (`204`), then `GET` it | `404`, "Task not found.", the project's stored tasks unchanged (only the other task left) | Pass |
| A task identifier that never existed is not found | `GET …/tasks/{random id}` | `404`, "Task not found.", the project's stored tasks unchanged | Pass |

**Commands and results:**

| Command | Result |
|---|---|
| `./mvnw test -Dtest=TaskApiPersistenceTest -Dskip.npm -Dskip.installnodenpm` | **5 tests, 5 passed, 0 failed**. The row counts were back as before after every test. |
| The same command, run a second time straight away | **5 passed**: the run doesn't depend on data left by an earlier one. |
| The same, with the title's minimum temporarily changed from 3 to 2 | **1 failed**, as expected: the title "ab" was no longer refused with the story's message. The change was then reverted, and `git diff` shows no change under `src/main`. |
| `./mvnw verify` | **BUILD SUCCESS**: 200 back-end tests (195 before, plus these 5), 0 failed, together with the browser application tests and `ModularityTest` |

**Tests corrected:** one. The first version of the non-member test checked that the task's identifier didn't appear anywhere in the answer, and it failed. The identifier was there only in the `instance` field, which repeats the path the client itself requested, and the contract's `Problem` schema allows that field. No task data was disclosed, so the test was wrong. It now checks that the answer has none of the task's fields and not its title.

**Defects found:** none in the behaviour under test. One difference between the code and the contract was found and resolved in the contract:
- **D1, `type` missing from error answers.** The contract's `Problem` schema listed `type` as required (`required: [type, title, status, detail]`, default `about:blank`), but the error answers have no `type` field, e.g. `{"detail":"Project not found.","instance":"…","status":404,"title":"Not Found"}`. RFC 9457 reads a missing `type` as `about:blank`, so the meaning is the same, and no user story or client relies on the field: the browser application reads only `detail`.
  - **Resolved (decided by the developer):** the contract was corrected rather than the code. `type` is no longer required, its description says it is omitted when it is `about:blank`, and the 72 error examples no longer show it.
  - **Checks after the change:** the contract still lints as valid, with the same single expected warning (`localhost`, AS03), and `./mvnw verify` passes.

## 12. End-to-end test: creating a task through the browser

On 2026-11-14 a user journey was automated in a real browser with Playwright 1.64.1 (Chromium), added with the developer's approval: **log in → open a project → create a task → find it on the board**.

**Test:** `frontend/e2e/create-task.e2e.ts`, configured by `frontend/playwright.config.ts`.

**What it runs against:** `frontend/e2e/server.mjs` starts, for every run:
- a throw-away PostgreSQL 17.10 in Docker, on a free port;
- the JAR just built, on port 8095, in an empty working directory, so a developer's `.env` is never read.

At the end the script stops both. The database is new every time, so no run depends on an earlier one.

**Test data:**
- **The account:** the demo account Ana. Her password comes from `MATRIZ_DEMO_PASSWORD_ANA` if it is set; otherwise the run makes one up and gives it to both the application and the test. No password is in the repository.
- **The project:** before the journey, a project with a unique name is created through the API, and it is deleted after it.

| Step | What the test does | Expected (user stories, wireframe) | Observed |
|---|---|---|---|
| Log in | Fills **Email** and **Password** on the log-in screen, clicks **Log in** | The home dashboard, heading "My projects" | Pass |
| Open the project | Clicks the test project's name on the dashboard | The project's board, headed with its name | Pass |
| Create a task | **New task**, fills **Title** with a unique title, **Create task** | — | Pass |
| Find it on the board | Looks in the board's column "To do" | The card is in "To do", shows "Unassigned", the column's count is 1, and the task is in neither "In progress" nor "Done" | Pass |

**How elements are found:**
- **Selectors:** only by role, label and visible text (`getByRole`, `getByLabel`), checked against the screens' accessibility tree. There are no CSS selectors, and no `data-testid` was added.
- **Waiting:** there are no fixed waits; the test uses Playwright's automatic waiting and assertions.
- **On failure:** a screenshot and a trace are kept, and only then.

**Commands and results:**

| Command | Result |
|---|---|
| `npm run e2e` (in `frontend/`), twice in a row | **1 passed** each time, about 10 s, each with a brand-new database; no container left behind (`docker ps -a --filter label=matriz-e2e` is empty) |
| The same, with the test temporarily expecting the card in "In progress" | **1 failed**, as expected, with `test-failed-1.png` and `trace.zip` kept. The screenshot shows the card in "To do" as "Unassigned", so the failure was the test's own change: a problem with the test, not a defect. The test was then restored. |
| `./mvnw verify -Dskip.e2e`, and `./mvnw verify -Dskip.npm -Dskip.installnodenpm` | The `npm-e2e` step is skipped |
| `./mvnw verify` | **BUILD SUCCESS** in 133 s: 200 back-end tests, 46 browser application tests, then the `npm-e2e` step: "1 passed" |

**Other browser, checked once:** the same test was run in **WebKit** (Safari's engine), with a temporary configuration that was deleted afterwards. **1 passed.** The build keeps Chromium only, as decided.

**Interrupting a run.** Each case was forced while the application was up, then the leftovers were checked (containers labelled `matriz-e2e`, a process on port 8095, the server script):

| How the run was interrupted | First version of `server.mjs` | After the correction |
|---|---|---|
| Ctrl+C (SIGINT to the whole process group) | Nothing left | Nothing left |
| `kill -9` of the Playwright runner and workers | The server script, the application on port 8095 and the container were left running. The next run refused to start: "http://localhost:8095/actuator/health is already used". | Nothing left: the script notices Playwright is gone within a second and cleans up. The next run passes straight away. |
| `kill -9` of the server script itself | Not tried | The application stopped with it; only the container was left, and the next run removed it (by its label) and passed. |

At first, the forced kill only hit the `npx` wrapper: the real runner went on, finished and cleaned up, so that case was repeated against the runner itself.

**Test infrastructure corrected:** `server.mjs`, in two ways (not production code):
- **It watches the process that started it,** and stops the application and the database when that process is gone.
- **It waits for the application to stop** (up to 20 s, then kills it) before it exits. Before, it exited at once, while Spring was still shutting down, so a run started straight afterwards could find port 8095 still taken.

**Tests corrected:** none. **Defects found:** none in the application.

**Environment limitations:**
- **Docker:** the end-to-end test needs Docker running, as the back-end tests already did.
- **Port:** it needs port 8095 free (or `E2E_PORT`).
- **First run:** it downloads Chromium once (about 100 MB).
