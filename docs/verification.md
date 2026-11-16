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
| `./mvnw verify -Dskip.e2e`, and `./mvnw verify -Dskip.npm -Dskip.installnodenpm` | The `npm-e2e` step is skipped. **Corrected on 2026-11-14 (code and security review):** this first check was not valid. It also passed `-DskipTests`, which skips the test steps by itself, and the shell passed the two flags as one argument. Run again without `-DskipTests` and with separate arguments (Java tests limited to `CookieDefaultsTest`, `frontend-maven-plugin` 2.0.2): with no flag, every npm step runs, the end-to-end test included; `-Dskip.e2e` skips only `npm-e2e`; `-Dskip.npm -Dskip.installnodenpm` skips the Node installation and every npm step. |
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

## 13. Continuous integration on GitHub Actions

On 2026-11-14 the CI workflow (`.github/workflows/ci.yml`) ran for the first time on GitHub, on its own commit `aa5ddbb`: [run 43652333374](https://github.com/vitorhsbarros/matriz/actions/runs/43652333374). Every job passed on the first run.

| Job | What it ran | Result | Duration |
|---|---|---|---|
| Build and tests | `./mvnw verify` on `ubuntu-24.04`, Java 21: 213 back-end tests, 46 browser application tests, `ModularityTest`, and the end-to-end test in Chromium | Pass (BUILD SUCCESS in 2 min 55 s) | 3 min 21 s |
| SAST (Semgrep) | Semgrep 1.183.0, the review's six rule sets: 239 rules on 101 files | Pass, 0 findings | 31 s |
| SAST (CodeQL, java-kotlin) | CodeQL, default queries, results uploaded to code scanning | Pass, 0 alerts | 1 min 55 s |
| SAST (CodeQL, javascript-typescript) | The same, for the browser application | Pass, 0 alerts | 1 min 20 s |
| SCA (OSV-Scanner) | OSV-Scanner 2.7.0 over a CycloneDX SBOM of the Maven dependencies (155) and `frontend/package-lock.json` (137) | Pass, no known vulnerabilities | 41 s |

**Code scanning:** GitHub lists both CodeQL analyses for `aa5ddbb`, each with 0 results, and the repository has 0 open alerts.

**The same results as locally:**
- **Tests:** the same numbers as the local `./mvnw verify`.
- **Semgrep 1.183.0:** the same 0 findings as the job's command run locally. Locally it had to run in its amd64 variant: the arm64 image fails on this Mac with an I/O error when it starts its engine, while the older 1.157.0 works.
- **OSV-Scanner:** the same 155 and 137 packages as locally.

**Not run before the push:** CodeQL had not been run locally (no CLI installed), so its first result is this run's.

**How the SCA job reads Maven:** OSV-Scanner can't read the versions in `pom.xml`, which come from Spring Boot's parent. When given the file, it filters out all 14 declared dependencies and still answers "No issues found", so that scan checked nothing. The job therefore has Maven write a CycloneDX SBOM (`cyclonedx-maven-plugin` 2.9.3, called on the command line only) and fails if it is empty.
- **Checked locally:** an SBOM with `log4j-core` 2.14.1 gives 8 known vulnerabilities and exit code 1, so the job fails on findings.

**The gates fail when they should:** a throw-away branch, `ci-gate-check`, had one commit that changed an expected message in `TaskTitleRuleTest`. The workflow was run on it with `workflow_dispatch`: [run 43652736040](https://github.com/vitorhsbarros/matriz/actions/runs/43652736040).
- **Build and tests failed**, with 3 failures in `TaskTitleRuleTest`. The `test-reports` artefact (108 KB) was uploaded.
- **The other four jobs passed**, as expected, since the change was only in a test.
- **Afterwards,** the branch was deleted locally and on GitHub. The failed run stays in the history as evidence.

**Later runs:** each later run is recorded in the [GitHub Actions history](https://github.com/vitorhsbarros/matriz/actions/workflows/ci.yml), which is the evidence from now on. This section is updated only when the CI itself changes in a relevant way.

## 14. Application container image

On 2026-11-15 the application was put in a container image, following step 1 of [delivery-plan.md](delivery-plan.md). Built and run on the developer's machine (arm64, Docker 29.1.3).

**Files:**
- **`Dockerfile`:** two stages.
  - **Build:** `eclipse-temurin:21.0.13_9-jdk-noble` runs `./mvnw package` without compiling or running the tests, which need Docker and run in CI.
  - **Final:** `eclipse-temurin:21.0.13_9-jre-noble` with only the JAR. It runs as user 10001, exposes port 8080, and has a health check on `/actuator/health`.
  - Both base images are pinned by the digest of their multi-platform index.
- **`.dockerignore`:** an allow-list. Only the Maven wrapper, `pom.xml`, `src/main` and the browser application's sources reach the build. `.env`, `.git`, `target/`, `node_modules/` and the tests stay out.

**Commands and results:**

| Check | Command | Result |
|---|---|---|
| Build | `docker build -t matriz:local .` | Success, about 54 s on the first build (Maven 33 s, browser application included) |
| Size | `docker images matriz:local` | 153 MB compressed, 541 MB on disk |
| Build context | A throw-away build that lists what `COPY . ` receives | 110 files: `.mvn`, `frontend` (sources only), `mvnw`, `pom.xml`, `src/main`. No `.env`, `.git`, `target`, `node_modules`, `src/test`, `e2e` or test results |
| Start-up | `docker run` on the Compose network (`matriz_default`), `MATRIZ_DB_URL` pointing to `postgres`, the other values from `.env` with `--env-file`, demo seed on, port `8082:8080` | `docker ps`: `Up (healthy)`. `docker logs`: Flyway "Schema "public" is up to date", "Started MatrizApplication in 3.8 seconds", 0 lines with ERROR or WARN |
| Health | `curl http://localhost:8082/actuator/health` | `{"status":"UP"}` |
| Empty database | The image against a throw-away PostgreSQL 17.10 with no data, without the demo seed | The six migrations applied ("now at version v4.1"), started, healthy |
| Non-root user | `docker run --rm --entrypoint id matriz:local` | `uid=10001(matriz) gid=10001(matriz)` |
| Read-only application | `ls -la /app`, then `touch /app/.env` as the application user | The JAR belongs to root, mode `r--r--r--`. The write is refused: "Permission denied" |
| Contents | `find /` for `.env`, `*.java`, `pom.xml`, `package.json`, `node`, `npm`, `mvnw`, `.m2`, `javac`, `node_modules` | None found |
| Secrets | `docker history --no-trunc` and `docker image inspect`, searched for `password`, `secret`, `change-me`, `MATRIZ_DB`, `MATRIZ_DEMO` | 0 matches. The image sets no application variable |

**Behaviour kept:** checked with `curl` against the container, as the browser does:

| Step | Expected | Observed |
|---|---|---|
| `GET /` and its script | The browser application | 200, the page and `assets/index-….js` (200, `text/javascript`) |
| `POST /api/session` as Ana | Logged in, session cookie | 200, `MATRIZ_SESSION` set, `HttpOnly`. `Secure` is off because the local `.env` sets `MATRIZ_COOKIE_SECURE=false` for `http://localhost` |
| `GET /api/projects` | Ana's projects | 200 |
| Create a task without the CSRF header | Refused | 403 |
| Create a task with the CSRF header | Created | 201 |
| Read the task back | The same title, status "To do" | 200, the same title, `TODO` |
| Delete the task, then log out | Clean-up | 204, 204 |

The test task was deleted, so the database was left as it was. The container was then removed. The image and the database data were kept.

**Differences from the plan:**
- **Port:** the planned port 8081 was taken by a local `java -jar` of the application, which was left running, so the checks used 8082.
- **Health check in the image:** the plan left it open (section 8). `curl` comes with the base image, so the image has one.

**Not checked:**
- `./mvnw verify` was not run: the new files are not part of the Maven build.
- The amd64 image: it is built in CI, in step 3 of the plan.
- The image wasn't scanned for known vulnerabilities in its operating system packages. No scanner is part of the approved tools.

## 15. Running the whole application with Docker Compose

On 2026-11-15 the application was added to `compose.yaml`, following step 2 of [delivery-plan.md](delivery-plan.md). Run on the developer's machine (arm64, Docker 29.1.3).

**What changed:**
- **`compose.yaml`:**
  - **A new service, `app`:** in the Compose profile `app`, built from the `Dockerfile` as `matriz:local`.
    - It starts only once `postgres` is healthy, and uses the image's own health check.
    - It gets an explicit list of settings, not the whole `.env`: its own `MATRIZ_DB_URL` (to the `postgres` service), the database user and password, the demo seed and passwords, and `MATRIZ_COOKIE_SECURE`.
    - It is published on `127.0.0.1:${MATRIZ_APP_PORT:-8080}`.
    - It runs with no Linux capabilities, `no-new-privileges`, and a read-only file system apart from a `/tmp` in memory.
  - **Both services:** on the network `matriz`, with `restart: unless-stopped`. PostgreSQL is published on `127.0.0.1` only (it was on every interface) and has `no-new-privileges`. The `matriz-postgres` volume is unchanged.
- **`.env.example`:** adds `MATRIZ_APP_PORT`, and a note that the application container ignores `MATRIZ_DB_URL`.

**Commands and results:**

| Check | Command | Result |
|---|---|---|
| Valid file | `docker compose config --quiet` | Passes |
| Services | `docker compose config --services`, without and with `--profile app` | `postgres` only; `postgres` and `app` |
| Compatibility | `docker compose up -d --wait` | Only `postgres` starts, `healthy`, on `127.0.0.1:5432`. The existing data was kept |
| Start-up | `MATRIZ_APP_PORT=8082 docker compose --profile app up -d --build --wait` | Both `healthy`. `app` started 28 s after `postgres`, once it was healthy. Logs: Flyway on `jdbc:postgresql://postgres:5432/matriz`, schema up to date, "Started MatrizApplication in 5.0 seconds". 0 lines with ERROR, WARN or FATAL |
| Reachable | `curl http://localhost:8082/actuator/health`, `GET /` and its script | `UP`. The page and `assets/index-….js` answer 200 |
| Communication | `docker network inspect matriz_matriz`, and `getent hosts postgres` inside `app` | Both containers on the network. `postgres` resolves to the database container |
| Settings in `app` | `docker inspect` (variable names only) | The listed `MATRIZ_*` variables and the image's own (`JAVA_HOME`, `PATH`, locale). Not `MATRIZ_DB_NAME` nor `MATRIZ_APP_PORT` |
| Least privilege | `docker inspect`, `/proc/1/status`, `touch` inside `app` | User 10001, every capability set empty, `no-new-privileges`. `/app` and `/opt` refuse writes ("Read-only file system"), `/tmp` accepts them |
| Exposure | `docker compose --profile app ps` | `127.0.0.1:8082->8080` and `127.0.0.1:5432->5432` |
| No secrets in files | Search of `compose.yaml` and `.env.example` | `compose.yaml` only refers to variables. `.env.example` has only the `change-me-…` examples |

**Main features:** the same `curl` checks as in [section 14](#14-application-container-image): Ana logs in (200, `MATRIZ_SESSION`, `HttpOnly`), the projects are listed (200), a task without the CSRF header is refused (403), with it the task is created (201) and read back with the same title in "To do" (200), then it is deleted (204) and Ana logs out (204).

**Data kept after recreating the containers:**

| Step | Result |
|---|---|
| Create a task as Ana | 201 |
| `docker compose --profile app down` (without `-v`) | Both containers and the network removed. The volume `matriz_matriz-postgres` stayed |
| `docker compose --profile app up -d --wait` | Both `healthy`, with new container IDs. 0 lines with ERROR, WARN or FATAL |
| Read the task back | 200, the same title |
| Delete it, read again | 204, then 404 |

The test tasks were deleted, so the database was left as it was. At the end the `app` service was removed and only `postgres` was left running, as before.

**Environment:**
- **Port 8080** is taken on this machine by `vagrant-vmware-utility`, and port 8081 by a local `java -jar` of the application, which was left running and stayed `UP`. So the checks set `MATRIZ_APP_PORT=8082` for the command, which also checks that the variable works. The local `.env` wasn't changed.
- **The old network** `matriz_default`, from the earlier `compose.yaml`, was left behind empty. It isn't used any more.

**Not checked:**
- `./mvnw verify` was not run: the changes are not part of the Maven build. The end-to-end test starts its own PostgreSQL and doesn't use `compose.yaml`.
- A log-in from a browser was not tried. The checks used `curl`, as in section 14.

## 16. Continuous integration of the image

On 2026-11-15 the CI workflow got a fifth job, **Image (build, scan and smoke test)**, following step 3 of [delivery-plan.md](delivery-plan.md). It runs only after the four jobs of [section 13](#13-continuous-integration-on-github-actions) have passed, and has `contents: read` only. Nothing is published.

**What the job does:**
1. **Build:** Buildx (`docker/setup-buildx-action` v4.4.1) and `docker/build-push-action` v7.4.0, approved on 2026-11-15 and pinned by commit, with the layers cached in GitHub Actions. The image is loaded on the runner (amd64) and pushed nowhere.
2. **Scan:** the OSV-Scanner of the `sca` job, v2.7.0, run as the image its action uses, pinned by digest, over the saved image. The job fails on any vulnerability that already has a fix. The others are listed in the log and in the run's summary.
3. **Configuration:** a `.env` with throw-away values made in the job (`openssl rand`), masked with `::add-mask::` and deleted at the end.
4. **Smoke test:** `docker compose --profile app up --wait`, then `scripts/image-smoke-test.sh`, the checks of sections 14 and 15 as a script. It stops at the first failed check and names it.
5. **Clean-up:** the containers' logs are printed only on failure. `docker compose down --volumes` and the deletion of `.env` always run.

**Change to the image:** the first local scan found 3 vulnerabilities with a fix, all from one advisory, USN-9065-1 (glibc: `libc6`, `libc-bin`, `locales` 2.39-0ubuntu8.9, severity 7.0, fixed in 2.39-0ubuntu8.12). It was published on 2026-11-12, the day after the base image was built. With the developer's approval, the final stage of the `Dockerfile` now installs the Ubuntu security updates (`apt-get upgrade`). After that: 0 with a fix, 179 without one. The image grew from 153 MB to 165 MB compressed.

**Local checks:**

| Check | Result |
|---|---|
| `scripts/image-smoke-test.sh` against `docker compose --profile app` (port 8082), before and after the `Dockerfile` change | 11 checks passed, "All checks passed." |
| The same with a wrong port (`MATRIZ_APP_PORT=8099`) | Exit code 1, "FAIL  /actuator/health answers UP" |
| The same without the demo password (`ENV_FILE=/dev/null`) | Exit code 1, naming `MATRIZ_DEMO_PASSWORD_ANA` |
| `actionlint` 1.7.12 on `ci.yml` | No findings |
| The scan step's parsing, run on a local scan result | 3 fixable before the change (step fails), 0 after (step passes) |

**Runs on GitHub:**

| Run | Commit | Trigger | Result |
|---|---|---|---|
| [43771873110](https://github.com/vitorhsbarros/matriz/actions/runs/43771873110) | `762b477` on `main` | `push` | **All six jobs passed.** `image` started at 22:57:19, two seconds after the last check (`build`) ended, and took 2 min 01 s. Scan: "Vulnerabilities with a fix: 0. Without a fix yet: 179." Smoke test: the 11 checks passed |
| [43772372001](https://github.com/vitorhsbarros/matriz/actions/runs/43772372001) | `eff9474`, one assertion in `TaskTitleRuleTest` expecting a wrong message | `pull_request` ([#1](https://github.com/vitorhsbarros/matriz/pull/1)) | **Failed, as expected.** `Build and tests`: 213 tests, 3 failures, all in `TaskTitleRuleTest`. The `test-reports` artefact (108 KB) was kept. `image` was **skipped**. The other four jobs passed |
| [43772687062](https://github.com/vitorhsbarros/matriz/actions/runs/43772687062) | `c9e9598`, the assertion restored | `pull_request` (#1) | **All six jobs passed.** `image` took 1 min 36 s, with the cached layers |

Pull request #1 was then closed without merging, and its branch `ci-image-gate-check` deleted locally and on GitHub. The runs stay in the history as evidence.

**No secrets in the log:** the log of the `image` job on `main` (1,802 lines) has no 48-character hexadecimal value, the form of the generated passwords. The `.env` lines appear only as the step's source, with variable names.

**Not checked:**
- **The Java libraries in the image:** the scan sees only the Ubuntu packages. The libraries are inside the Spring Boot JAR, where OSV-Scanner doesn't look. They are the same as the Maven dependencies that the `sca` job scans on every run, through the SBOM. Extracting the JAR in the image would make them visible to this scan too. It was tried locally (93 Maven packages found, 0 vulnerabilities) and not kept, by the developer's decision, because it adds no coverage.
- **The Java runtime:** Temurin is installed from an archive, not from an Ubuntu package, so neither scan sees it.
- **The browser:** the smoke test uses `curl`. The end-to-end test in the browser runs in the `build` job, against the JAR.

## 17. Publishing the image to the GitHub Container Registry

On 2026-11-15 the CI workflow started publishing the image when a GitHub release is published, following step 4 of [delivery-plan.md](delivery-plan.md) (Challenge C). It was shown with a test pre-release.

**What changed in `ci.yml`:**
- **Trigger:** `release: published`, besides `push`, `pull_request` and `workflow_dispatch`.
- **`image` job:** the image gets the OCI labels `org.opencontainers.image.source`, `.revision` (the commit) and `.version` (the tag or branch). On a release only, the image saved for the scan, the one then tested, is kept as the artifact `image` for one day.
- **New `publish` job:**
  - It runs only on releases, after `image`, with `contents: read` and `packages: write`. It is the only job that can write packages.
  - It downloads the artifact (`actions/download-artifact` v8.0.2, pinned by commit) and loads the image. It refuses a release tag that isn't a version (`vX.Y.Z` or `vX.Y.Z-suffix`).
  - It logs in to `ghcr.io` with the workflow's `GITHUB_TOKEN` through `--password-stdin`, pushes the tags, then logs out. No secret is stored.
  - Tags: the version without the `v`, and `sha-<commit>`. `latest` only for a release that isn't a pre-release.
  - It writes the digest and the tags to the run's summary.

**Local checks:**

| Check | Result |
|---|---|
| `actionlint` 1.7.12 | No findings. Only `publish` has `packages: write` (and only `sast-codeql` has `security-events: write`, as before) |
| The tag logic in bash 5.2 | `v1.1.0-rc.1` (pre-release) → `1.1.0-rc.1`, `sha-762b477`; `v1.1.0` → the same plus `latest`; `release-1` → refused |

**Runs on GitHub:**

| Run | Trigger | Result |
|---|---|---|
| [43774189361](https://github.com/vitorhsbarros/matriz/actions/runs/43774189361), `532bad8` | `push` to `main` | Every job passed. **`publish` was skipped.** No image artifact. The only artifact is Buildx's build record (`.dockerbuild`, 38 KB), which holds no image |
| [43774611918](https://github.com/vitorhsbarros/matriz/actions/runs/43774611918), `532bad8` | `release`, [v1.1.0-rc.1](https://github.com/vitorhsbarros/matriz/releases/tag/v1.1.0-rc.1) (pre-release) | Every job passed. `image` started after the four checks, and `publish` started at 23:28:45, after `image` (35 s). "Published ghcr.io/vitorhsbarros/matriz@sha256:4f7eed18…d5 as: …:1.1.0-rc.1 …:sha-532bad8". The log has no token (`ghs_`: 0 matches) |

**The published package** ([ghcr.io/vitorhsbarros/matriz](https://github.com/users/vitorhsbarros/packages/container/package/matriz)):

| Check | Result |
|---|---|
| Visibility and link | **Public**, as the repository, and linked to `vitorhsbarros/matriz` |
| Tags | `1.1.0-rc.1` and `sha-532bad8` on one version. No `latest`, as expected for a pre-release |
| Pull without logging in | `docker pull --platform linux/amd64 ghcr.io/vitorhsbarros/matriz:1.1.0-rc.1` succeeds |
| Same image | The pulled digest is `sha256:4f7eed188d33aeb01aedfb817480a2201a96990fa9e620455d0ca94c905167d5`, the one in the run's summary |
| Traceability | Labels: `source=https://github.com/vitorhsbarros/matriz`, `revision=532bad818dbd135ae4cdd7f18d4c8893baffda3c`, `version=v1.1.0-rc.1`. Architecture amd64, user 10001 |
| It runs | Tagged as `matriz:local` and started with `docker compose --profile app up --wait --no-build` (amd64 under emulation on the arm64 Mac, started in 14.9 s). `scripts/image-smoke-test.sh`: all 11 checks passed |

Afterwards the `app` service and the pulled image were removed, and the local arm64 image was built again. Only `postgres` was left running, as before.

**Kept:** the pre-release `v1.1.0-rc.1`, its tag, and the package version, as the evidence of this check.

**Not checked:**
- **arm64:** the published image is amd64 only. Multi-platform images are out of scope.
- **A final release:** `latest` was checked only through the tag logic, not with a published final release.
- **Provenance attestations:** the image isn't signed and has no attestation. Its source can be traced only through its labels and the run that published it.
- **The credentials warning:** `docker login` warns that the credentials are stored unencrypted on the runner. The runner is thrown away after the job, and the job logs out after pushing.

## 18. Observability: structured logs, liveness and readiness

On 2026-11-15 the application got structured logs in its container image, and public liveness and readiness checks (Challenge B). No dependency was added: both are part of Spring Boot 4.1.2.

**What changed:**
- **`application.yaml`:** the health probes are enabled explicitly.
  - **Liveness** (`/actuator/health/liveness`) is `livenessState` only. A database failure doesn't make it fail, so a platform doesn't restart the application for that.
  - **Readiness** (`/actuator/health/readiness`) is `readinessState` and `db`: the application isn't ready while the database can't be reached.
  - The details stay hidden.
- **`SecurityConfig`:** `/actuator/health/liveness` and `/actuator/health/readiness` can be read without a session, besides `/actuator/health`. Nothing else is opened. Only the `health` endpoint is exposed, as before.
- **`Dockerfile`:**
  - `LOGGING_STRUCTURED_FORMAT_CONSOLE=ecs`, so the container writes its logs as JSON lines in the Elastic Common Schema (ECS 8.11). It can be overridden at run time like any setting;
  - the `HEALTHCHECK` now uses readiness.
- **`scripts/image-smoke-test.sh`:** also checks that the three health URLs answer `UP` with no details, and that every line the application has logged is ECS JSON (`@timestamp`, `log.level`, `message`, `service.name`, `ecs.version`). It runs in the CI `image` job, as before.
- **New test, `HealthChecksTest`:** the three health URLs answer 200 and `UP` without a session, with no `components` or `details`. `/actuator`, `/actuator/info`, `/actuator/env`, `/actuator/health/db` and `/actuator/health/diskSpace` answer 401.

**Personal data in the logs, reviewed before the change:**
- **The code:** the application code has no log statement (no logger, no `System.out`). Every log line comes from Spring, Hibernate, Flyway, Hikari or Tomcat.
- **The output:** a failed log-in, a log-in, a task creation with a title and a description, a malformed body (400), an unknown task (404) and a deletion **wrote no log line at all**.
- **The search:** the start-up log (29 lines, all INFO, with the demo seed on) has no email address, and no "password", "secret" or "token". The same searches for the email, both passwords, the session ID, the CSRF token, the task's title and description, and the malformed body found nothing.
- **Findings:** none. The database host and name appear in Flyway's start-up line (`jdbc:postgresql://postgres:5432/matriz`). They aren't secret.

**Commands and results:**

| Check | Result |
|---|---|
| `./mvnw test -Dtest=HealthChecksTest` | 8 tests, 0 failures. Before the change, liveness and readiness answered 401 |
| `./mvnw verify` | **BUILD SUCCESS** in 2 min 10 s: 221 back-end tests (213 + 8), 46 browser application tests, `ModularityTest`, and the end-to-end test: "1 passed" |
| `docker compose --profile app up -d --wait`, then `scripts/image-smoke-test.sh` (port 8082) | `healthy`. **All 12 checks passed**, among them "/actuator/health, its liveness and its readiness answer UP, with no details" and "every log line is ECS JSON (29 lines)" |
| The same with the logs switched to the Logstash format (a Compose override file outside the repository) | Exit code 1: "FAIL  every log line is ECS JSON (29 of 29 are not)" |
| A local `java -jar` of the new JAR (port 8090) | Plain-text logs (0 JSON lines out of 50). Liveness and readiness `UP` |

**Database down,** with `docker compose stop postgres` while `app` ran:

| Time after stopping it | Readiness | Liveness | Container |
|---|---|---|---|
| +30 s, +65 s, +100 s | `DOWN`, **503**, after **30 s** each | `UP`, 200, at once | — |
| +105 s | | | `unhealthy`, still running, 0 restarts |
| `docker compose start postgres`, +2 s | `UP`, 200 | `UP` | `healthy` again |

The 50 log lines written during the outage are all JSON: 48 WARN (the database health check, Hikari's "Connection is not available, request timed out after 30006ms", the health check that "took 30019ms to respond") and 2 ERROR (Spring Session's scheduled clean-up of expired sessions, which needs the database). None has an email address, password, secret or token.

**Open point:** while the database is down, readiness takes **30 s** to answer, because the database check waits for Hikari's connection time-out (30 s by default). The image's health check gives up after 3 s and counts that as a failure, so the result is still right. A platform probing with a shorter time-out would see a time-out instead of a 503. A shorter connection time-out (e.g. `spring.datasource.hikari.connection-timeout`) would change that, but also how long a request waits for a connection under load. It wasn't changed: it is left for the developer to decide.

Afterwards the `app` service was removed and only `postgres` was left running, with its data. The local `java -jar` on port 8081 stayed `UP` throughout (it lost the database while it was stopped, like the container).

**CI run:** [43777138671](https://github.com/vitorhsbarros/matriz/actions/runs/43777138671), on `a7c7b32` (`push` to `main`). Every job passed, and `publish` was skipped. In the `image` job, the scan found 0 vulnerabilities with a fix (179 without one), and the smoke test passed all 12 checks, among them "every log line is ECS JSON (37 lines)".

**Not checked:**
- **A log collector:** no log collector or platform was used. The format was checked line by line with `jq`.

## 19. Release v1.1.0

On 2026-11-16 the Lab 05 delivery was released as `v1.1.0` ([docs/delivery-plan.md, "Delivery"](delivery-plan.md#10-delivery)).

**Documented commands, run as written** before the release, with `MATRIZ_APP_PORT=8082` because ports 8080 and 8081 are taken on this machine:

| Command (README "Running with Docker", AGENTS.md §10) | Result |
|---|---|
| `docker compose --profile app build` | `matriz:local  Built` |
| `docker compose --profile app up -d --wait` | `postgres` and `app` `Healthy` |
| `/actuator/health`, `/liveness`, `/readiness` | 200, `UP`. `/actuator/health` also lists the groups `liveness` and `readiness`, as the README says |
| `docker compose logs app`, and `docker compose logs --no-log-prefix app \| jq -r .message` | ECS JSON lines; the second shows only the messages ("Starting MatrizApplication v0.1.0-SNAPSHOT …") |
| `scripts/image-smoke-test.sh` | "All checks passed." (12 checks) |
| `docker compose --profile app down`, then `docker compose up -d --wait` | Both containers removed, the volume kept. Only `postgres` started again, with its data (3 users) |

**Runs and release:**

| What | Result |
|---|---|
| Push run [43785032983](https://github.com/vitorhsbarros/matriz/actions/runs/43785032983), `84b45ca` | Every job passed. `publish` skipped |
| Tag `v1.1.0` and release [v1.1.0](https://github.com/vitorhsbarros/matriz/releases/tag/v1.1.0) (final, latest) | The tag points to `28b0bc9`, the commit that adds the changelog, the release notes and the "Delivery" section |
| Release run [43785057302](https://github.com/vitorhsbarros/matriz/actions/runs/43785057302) | Every job passed. `image` ran after the four checks, `publish` after `image`. "Published ghcr.io/vitorhsbarros/matriz@sha256:221e9dc0…70c5 as: …:1.1.0 …:sha-28b0bc9 …:latest". No token in the log (`ghs_`: 0) |

**The published image:**

| Check | Result |
|---|---|
| Package versions | `sha256:221e9dc087037fc425ba492bc43edc6944a1655bf50750e4776ff50dafc570c5`: `1.1.0`, `sha-28b0bc9`, `latest`. The pre-release keeps its own version (`1.1.0-rc.1`, `sha-532bad8`) |
| `docker pull ghcr.io/vitorhsbarros/matriz:1.1.0`, as in the README, without logging in | Succeeds on the arm64 Mac without `--platform`: the amd64 image, the only one, is fetched |
| Digest and labels | The pulled digest is the one in the run's summary, and so is `latest`'s. `revision=28b0bc906e83216e190464933f3750d7b36141e7`, `version=v1.1.0` |
| `docker tag … matriz:local`, `docker compose --profile app up -d --wait --no-build`, then `scripts/image-smoke-test.sh` | `Healthy`, and all 12 checks passed under emulation |

Afterwards the `app` service and the pulled images were removed, the local arm64 image was built again, and only `postgres` was left running.

**The release was published twice.** It was first tagged on `3425dbe` (run [43778979924](https://github.com/vitorhsbarros/matriz/actions/runs/43778979924), digest `d73fe1ed…`). For exercise 5, the three last commits were rebuilt on `a6e40f6` as "Lab 05, exercise 5" with the changelog and the release notes added, and `main` was force-pushed with the developer's approval. The release and the tag `v1.1.0` were then deleted and created again on `28b0bc9`, so CI published the image again (above). The earlier image version, left with only the tag `sha-3425dbe` of a commit no longer on `main`, was deleted from the package, with the developer's approval.

**The API documentation checked against the code** (Lab 05, exercise 5), against the application in Docker Compose with the demo accounts:

| Check | Result |
|---|---|
| Operations: method and path of every operation in `openapi.yaml` and of every controller mapping | The same 30 operations on both sides |
| Success status of every operation (`201` and `204` from the code's annotations, `200` otherwise) | The same in all 30 |
| `401` without a session (a wrong password for `POST /api/session`) | All 30 operations |
| `404` with ids that don't exist | All 23 operations that document it |
| `400` with a malformed JSON body | All 12 operations that document it |
| `403` without the CSRF header | All 20 operations that document it, "The request could not be verified. Please reload and try again." |
| `403` for a member who isn't the manager or the author (Rui) | All 10 operations that document it, with the messages of the user stories |
| `422` with invalid data | All 15 operations that document it, with the messages of the user stories |
| `409` | All 4 operations, with the messages of the user stories. Each is also covered by a test: `MembersApiTest.someoneWhoIsAlreadyAMemberIsRefused`, `TeamsApiTest.aNameAlreadyInUseIsRefusedIgnoringCaseAndSpaces`, `TeamsApiTest.renameErrors` and `TeamsApiTest.memberErrors` |

112 requests, no difference from `openapi.yaml`. The tasks, comment and teams created for the check were deleted.

## 20. Release v1.1.1: the open points of v1.1.0

On 2026-11-16, at the developer's request, the open points of `v1.1.0` ([delivery-plan.md, section 10](delivery-plan.md#10-delivery)) were solved, except PD13, which is the developer's decision. Released as the patch `v1.1.1`.

**What changed:**
- **`pom.xml`:**
  - `spring-boot-starter-data-jpa` replaced by `spring-boot-starter-jdbc`. The code had no `@Entity`, repository or `EntityManager`, so JPA validated nothing;
  - version `1.1.1` (was `0.1.0-SNAPSHOT`);
  - `<finalName>matriz</finalName>`, so the JAR is `target/matriz.jar`.
- **`application.yaml`:** the `spring.jpa` block removed. `spring.datasource.hikari.connection-timeout: 5000` (milliseconds; 30 000 by default).
- **`Dockerfile`, `frontend/e2e/server.mjs`, README, AGENTS.md:** the new JAR name.
- **`ci.yml`, `publish` job:**
  - a provenance attestation of the pushed image (`actions/attest-build-provenance` v4.2.2, pinned by commit), also pushed to the registry;
  - new permissions, in this job only: `id-token: write` and `attestations: write`;
  - the log-out moved to a final step that always runs.
- **Documents:** architecture.md §5 (JDBC, the attestation), `docs/deployment-options.md` (PD13 compared, not decided), CHANGELOG, release notes.

**Local checks:**

| Check | Result |
|---|---|
| `./mvnw clean verify` | **BUILD SUCCESS** in 2 min 21 s: 221 back-end tests, 46 browser application tests, `ModularityTest`, and the end-to-end test against `target/matriz.jar`. No line of the build mentions Hibernate or JPA. The first run failed at start-up because the time-out was written `5s`, which Hikari doesn't read (it takes milliseconds); corrected to `5000` |
| JAR | `target/matriz.jar`, 31.8 MB (60.6 MB before) |
| Image (`docker compose --profile app build`) | 139 MB compressed (165 MB before) |
| `docker compose --profile app up -d --wait` (port 8082) | `Healthy`. "Starting MatrizApplication v1.1.1", `service.version` `1.1.1`, "Started MatrizApplication in 2.188 seconds" |
| `scripts/image-smoke-test.sh` | All 12 checks passed |
| `docker compose stop postgres` | Readiness `DOWN` 503 in 0.02 s (the pool's connections failed at once), then in **5.03 s**; liveness `UP` 200; the container turned `unhealthy`. After `docker compose start postgres`: readiness `UP`, `healthy` |
| `actionlint` | No findings. Write permissions: `security-events` in `sast-codeql`; `packages`, `id-token` and `attestations` in `publish` only |

**Runs, release and published image:**

| Check | Result |
|---|---|
| Push run [43788006059](https://github.com/vitorhsbarros/matriz/actions/runs/43788006059), `ef0975e` | Every job passed. `publish` skipped |
| Tag `v1.1.1` and release [v1.1.1](https://github.com/vitorhsbarros/matriz/releases/tag/v1.1.1) (final, latest) | The tag points to `ef0975e`, the commit with the changelog and the release notes |
| Release run [43788641658](https://github.com/vitorhsbarros/matriz/actions/runs/43788641658) | Every job passed. In `publish`: the push, "Attestation created for ghcr.io/vitorhsbarros/matriz@sha256:820fcecb…b7da" with its entry in Sigstore's public log, and the log-out. No token in the log (`ghs_`: 0) |
| `gh attestation verify oci://ghcr.io/vitorhsbarros/matriz:1.1.1 -R vitorhsbarros/matriz` | Exit code 0. SLSA provenance v1 of `sha256:820fcecb…b7da`, signed through `https://token.actions.githubusercontent.com`, built by `.github/workflows/ci.yml@refs/tags/v1.1.1` from `ef0975ee1d5603c2e8308dcc648e7331be2ea770`, in run 43788641658 |
| The same with another repository (`-R vitorhsbarros/not-matriz`), and on `1.1.0`, published before the attestation existed | Both refused (no attestation found) |
| Package versions | `sha256:820fcecb046131140ac4d380a5a4a492af0361ba140273c732fe694f82a0b7da`: `1.1.1`, `sha-ef0975e`, `latest`. `1.1.0` and `1.1.0-rc.1` keep their own versions. Two more versions hold the attestation in the registry: one tagged `sha256-820fcecb…`, one untagged |
| `docker pull ghcr.io/vitorhsbarros/matriz:1.1.1`, `docker tag … matriz:local`, `docker compose --profile app up -d --wait --no-build` (port 8082) | The digest of the run's summary, and so is `latest`'s. Labels `revision=ef0975e…`, `version=v1.1.1`. "Starting MatrizApplication v1.1.1", `service.version` `1.1.1`. Started in 9.9 s, under amd64 emulation |
| `scripts/image-smoke-test.sh` on it | All 12 checks passed |

Afterwards the `app` service and the pulled images were removed, the local arm64 image was built again, and only `postgres` was left running.

**Still open:** PD13 (compared in [deployment-options.md](deployment-options.md), not decided), the image for amd64 only, and the Java runtime outside both scans.

## 21. Deployment on Render

On 2026-11-16 Matriz was deployed on Render (PD13), for the final presentation: **https://matriz-2s4n.onrender.com**.

**How:**
- **Release `v1.2.0`:**
  - `MATRIZ_DB_URL` accepts a PostgreSQL connection string, which Render gives;
  - [render.yaml](../render.yaml), the Blueprint: a free PostgreSQL 17 and a free web service, run from the image `ghcr.io/vitorhsbarros/matriz:1.2.0`, both in Frankfurt.
- **The developer, in Render's dashboard:**
  - connected GitHub;
  - New → Blueprint, then this repository;
  - entered the three demo passwords, then Apply.
- **The assistant** had no access to the Render account, and saw no password.

**Render's documentation checked** (2026-11-16):
- [the free plan](https://render.com/docs/free);
- [the Blueprint specification](https://render.com/docs/blueprint-spec);
- [web services and their port](https://render.com/docs/web-services);
- [deploying an image](https://render.com/docs/deploying-an-image).

`render.yaml` was also checked against Render's JSON schema (`https://render.com/schema/render.yaml.json`): every key and value it uses is in the schema, whose objects refuse unknown keys. The schema allows `host` and `port` in `fromDatabase`, which the specification page doesn't list; the connection string was used, as the plan was made from the page.

**Local checks before the release:**

| Check | Result |
|---|---|
| `./mvnw clean verify` | BUILD SUCCESS: 232 back-end tests (11 new, `PostgresUrlEnvironmentPostProcessorTest`), 46 browser application tests, the end-to-end test |
| The image with `MATRIZ_DB_URL=postgresql://postgres/matriz` and the user and password apart (Render's way) | `healthy`, Flyway on `jdbc:postgresql://postgres:5432/matriz` |
| The image with the credentials only inside the connection string | `healthy` |
| The image with an invalid connection string | Start-up stopped: "MATRIZ_DB_URL is not a valid JDBC URL or PostgreSQL connection string … Its value is not shown because it may contain a password." The password appears 0 times in the logs |
| `scripts/image-smoke-test.sh`, local mode | All checks passed |
| Remote mode over plain HTTP | Refused: "FAIL  BASE_URL uses HTTPS" |
| Semgrep `p/secrets` on the 9 changed files | 0 findings |

**CI and release:**
- **Push run** [43924302207](https://github.com/vitorhsbarros/matriz/actions/runs/43924302207) on `1df6aab`: the first attempt failed in `npm ci`, because the connection to the npm registry was reset (`ECONNRESET`) before any test ran. The second attempt passed every job.
- **Release** [v1.2.0](https://github.com/vitorhsbarros/matriz/releases/tag/v1.2.0) and its run [43926111311](https://github.com/vitorhsbarros/matriz/actions/runs/43926111311): every job passed. The image was published as `1.2.0`, `sha-1df6aab` and `latest` (`sha256:34435b92…d801`). `gh attestation verify` checks it: built at `refs/tags/v1.2.0` from `1df6aab`.

**The deployment, checked from outside** (2026-11-16):

| Check | Result |
|---|---|
| `/actuator/health`, `/liveness`, `/readiness` | 200, `UP`, about 0.1 s (the service was awake) |
| `/` and its script | 200, `text/html` and `text/javascript` |
| HTTPS | TLS 1.3 over HTTP/2. Certificate for `onrender.com` by Google Trust Services (WE1), valid until 2026-12-20, verified. `http://` answers 301 to `https://` |
| Security headers | `Strict-Transport-Security: max-age=31536000 ; includeSubDomains`, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Cache-Control: no-cache, no-store …`. HSTS appears here and not locally over HTTP, so the application sees the requests as secure behind Render's proxy: no `server.forward-headers-strategy` was needed |
| `/api/projects` without a session | 401, "Please log in to continue." |
| `BASE_URL=https://matriz-2s4n.onrender.com scripts/image-smoke-test.sh`, run by the developer with Ana's password in the environment | **All 7 checks passed:** the health checks, the page, Ana's log-in, a `Secure` and `HttpOnly` session cookie, 403 without the CSRF header, a task created, read back and deleted |
| In a browser, by the developer | Sara logs in and her dashboard is empty, since she is in no project. Ana creates a task in *Website* |

**Not checked:**
- the time to wake up after 15 minutes without traffic (Render documents about a minute);
- the free database's expiry, 30 days after it was created.
