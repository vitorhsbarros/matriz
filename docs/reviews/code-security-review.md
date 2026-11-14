# Code and security review

- **Date:** 2026-11-14
- **Reviewer:** Claude, acting as a senior application security engineer. The prompt is in [docs/prompts/code-security-review.md](../prompts/code-security-review.md).
- **Decisions:** Vitor Barros (human in the loop).
- **Status:** closed. F1 to F6 were fixed after the developer's decisions, F1 to F4 each with a regression test, and the checks first left unverified were run afterwards (see "Checks run after the fixes" and "Build tools").

## Scope

**Code review**, of two diffs:
- **`878a791~1..d70e650`**, the last application increment:
  - the R1 and R2 fixes;
  - the shared `Problems` helper in each module;
  - the `DataIntegrityViolationException` catches;
  - the `FOR UPDATE` locks on deletion.
- **`v1.0.0..HEAD`**, everything after the Lab 03 delivery:
  - unit, integration and end-to-end tests;
  - `frontend/e2e/server.mjs`, `playwright.config.ts` and `pom.xml`;
  - the contract correction.

**Security review:**
- access to projects, tasks, members, assignees, comments and teams, through all 30 operations in `openapi.yaml`;
- data exposure;
- secrets in the repository and its Git history;
- static analysis with Semgrep.

**Rules reviewed against:**
- SEC01–SEC11 and PRI01–PRI05 in [constraints.md](../constraints.md);
- ADR-0002;
- DoD02–DoD07, D07, D08 and SEC11 in [user-stories.md](../user-stories.md).

**Severity:**
- **Critical:** data of other users read or changed, or authentication bypassed.
- **High:** an access rule broken in some cases, or a secret exposed.
- **Medium:** a missing validation or wrong error handling that gives users wrong answers or server errors, without exposing or changing data.
- **Low:** a missing test, or a latent problem with no effect today.

**Confirmed** means reproduced, with the request and the answer shown below. **Suspected** means seen in the code but not reproduced.

## Summary

**No access-control, data-exposure or secret problem was found.** Every attempt to read or change a resource without permission was refused with the documented answer, and nothing changed.

The review found four problems, all about error handling and tests:

| ID | Finding | Status | Severity | Outcome |
|---|---|---|---|---|
| F1 | A NUL character in a text field gives `500 Internal Server Error` | Confirmed | Medium | Fixed |
| F2 | Broad `DataIntegrityViolationException` catches turn unrelated database errors into "Project not found." / "Task not found." | Confirmed | Medium | Fixed |
| F3 | No test covers a team deleted between the check and the saving of its assignment | Confirmed (by reading the tests) | Low | Test added |
| F4 | A NUL character in the dashboard's search (`?name=`) gives `500` | Confirmed | Medium | Fixed |
| F5 | The build installs Node v24.15.0, four security releases behind the current v24.22.3 | Confirmed | Medium | Fixed |
| F6 | `frontend-maven-plugin` 1.15.4 brings libraries with known vulnerabilities | Confirmed | Low | Fixed |

## Findings

### F1 — A NUL character in a text field gives `500 Internal Server Error`

- **Status:** confirmed. **Severity:** medium.
- **Files:**
  - `src/main/java/matriz/projects/internal/ProjectFields.java`;
  - `src/main/java/matriz/teams/internal/TeamsController.java` (team name);
  - `src/main/java/matriz/tasks/internal/TaskFields.java` (`title`, `description`);
  - `src/main/java/matriz/tasks/internal/CommentsController.java` (`text`);
  - `src/main/java/matriz/projects/internal/MembersController.java` (`email`).
- **Evidence:** logged in as Ana, in the throw-away environment, a JSON string containing `\u0000` was sent:

  | Request | Body | Answer |
  |---|---|---|
  | `POST /api/projects` | `{"name":"abc\u0000def"}` | `500`, Spring Boot's default error body (not Problem Details) |
  | `PATCH /api/projects/{id}` | same | `500` |
  | `PATCH /api/projects/{id}/tasks/{taskId}` | `{"title":"abc\u0000def"}` | `500` |
  | `POST /api/teams`, `PATCH /api/teams/{id}` | `{"name":"abc\u0000def"}` | `500` |
  | `POST /api/projects/{id}/members` | `{"email":"x\u0000@example.com"}` | `500` |
  | `POST /api/projects/{id}/tasks` | title or description with `\u0000` | `404` "Project not found." (see F2) |
  | `POST …/tasks/{taskId}/comments` | `{"text":"abc\u0000def"}` | `404` "Task not found." (see F2) |

  Nothing was stored: the project still had only its one task afterwards. PostgreSQL rejects the NUL character in text columns (SQL state `22021`), and the field validation lets it through.
- **Impact:**
  - **What breaks:** a valid JSON body gets a server error instead of a documented refusal, which contradicts DoD04 and DoD07.
  - **Logs:** each request leaves an exception in the server log.
  - **What doesn't happen:** no data is exposed (the `500` body has no stack trace or message) and no data is changed.
- **Proposed fix:** refuse text that contains a NUL character before it reaches the database.
  - **Where:** in the field rules of each module (`ProjectFields`, `TaskFields`, the team name and comment text rules, the member email).
  - **Which answer** is a **decision for the developer**, because the specification doesn't cover this case. Two options:
    - **(a)** treat it as an unreadable body: `400` "The request body is not valid." (DoD07);
    - **(b)** remove the character before the other rules apply, as leading and trailing spaces are removed (D01).
- **Proposed regression test:** a test in `ReviewRegressionTest` that sends `\u0000` in each text field (project name, task title and description, comment text, team name, member email). It checks the chosen answer and that nothing is stored.

### F2 — Broad catches turn unrelated database errors into "not found"

- **Status:** confirmed. **Severity:** medium.
- **Files:** each catch covers every `DataIntegrityViolationException`, not just a missing parent row (the foreign-key violation, SQL state `23503`).
  - `src/main/java/matriz/tasks/internal/TasksController.java:122–126`: any error becomes "Project not found.";
  - `src/main/java/matriz/tasks/internal/CommentsController.java:86–91`: any error becomes "Task not found.";
  - `src/main/java/matriz/tasks/internal/TaskAssignees.java:97–102`: any error becomes "Team not found.".
- **Evidence:**
  - **For tasks and comments:** a member creating a task with `\u0000` in the title gets `404` "Project not found.", and a comment with `\u0000` gets `404` "Task not found." (F1's table). Spring reports PostgreSQL's data exception (`22021`) as a `DataIntegrityViolationException`, which these catches take for a deleted project or task.
  - **For `TaskAssignees`:** its catch has the same form; it wasn't reproduced, because a team assignment carries no text.
- **Impact:**
  - **Wrong answers:** a member of an existing project is told the project doesn't exist, which is misleading and hides the real cause.
  - **Future errors hidden:** any later database rule (a new check constraint, a length that drifts from the field rules) would also show up as "not found" instead of an error.
- **Proposed fix:** in the three catches, map only a foreign-key violation (SQL state `23503`) to "not found", and rethrow anything else.
  - **Example:** a small check on the root cause's SQL state, or catching only the exception that Spring's translator uses for that state.
  - **Order:** F1 then stops these inputs before they reach the database. F2 makes sure the next unforeseen error isn't disguised.
- **Proposed regression test:** a unit test of the mapping. A `DataIntegrityViolationException` caused by SQL state `23503` gives "not found", and one caused by any other state (e.g. `22021`) is not turned into "not found". An integration test with `\u0000` can't show F2 on its own once F1 is fixed, so the unit test is what keeps it fixed.

### F3 — No test of a team deleted between the check and the saving of its assignment

- **Status:** confirmed by reading the tests. **Severity:** low.
- **File:** `src/main/java/matriz/tasks/internal/TaskAssignees.java:97–102`, the catch added in the last increment (R2).
- **Evidence:**
  - **Where the tests reach:** `AssigneesApiTest.aTeamThatDoesntExistCantBeAssigned` covers a team that doesn't exist, but that case is refused earlier, at line 93 (`teams.find(...)`). The catch is never reached.
  - **What `ReviewRegressionTest` covers:**
    - a team assigned **before** its deletion (`aTeamAssignedWhileItIsDeletedLeavesTheTask`);
    - the "after deletion" path for tasks and comments.
  - **The gap:** no test covers the "after deletion" path for a team.
- **Impact:** if the catch were removed or broken, a team deleted at that moment would give a `500` again, and no test would notice.
- **Proposed fix:** none in the production code.
- **Proposed regression test:** in `ReviewRegressionTest`, the same "held transaction" pattern as the comment test. Delete the team inside a held transaction, send the assignment, and expect `422` "Team not found." with no assignee row.

### F4 — A NUL character in the dashboard's search gives `500`

- **Status:** confirmed. **Severity:** medium.
- **Found:** after the review, while checking whether F1's fix covered every way text reaches the database. It doesn't: the search is a query parameter, not a JSON body.
- **File:** `src/main/java/matriz/tasks/internal/DashboardController.java:37` (`@RequestParam String name`, used in the search in `JdbcProjects`).
- **Evidence:** from a temporary probe test, deleted afterwards, logged in as a test user.

  | Request | Answer |
  |---|---|
  | `GET /api/projects?name=web` | `200` |
  | `GET /api/projects?name=a%00b` | `500` |
  | `GET /api/projects?name=%00` | `500` |

- **Impact:** the same as F1, on a read-only request. A server error with no data exposed, and an exception in the log.
- **Proposed fix:** a **decision for the developer**, because the specification doesn't cover this case. Two options:
  - **(a)** refuse the search with `400`;
  - **(b)** treat it as text that matches no project, and answer with an empty list.

  F1's message ("The request body is not valid.") speaks of the body, so it doesn't fit a query parameter as it is.
- **Proposed regression test:** in `ReviewRegressionTest`, `GET /api/projects?name=%00` gets the chosen answer and never `500`.

## Checks run

| Check | How | Result |
|---|---|---|
| Code review of both diffs | Read every change under `src/main`, `src/test`, `frontend/e2e`, `pom.xml` | F2 and F3; F1 was then found while confirming F2 |
| Access control, non-member | Sara, on "Website": the 19 project, member, task, assignee and comment operations | All `404` "Project not found.", nothing changed |
| Access control, member who isn't the manager | Rui, on "Website": edit and delete the project, add and remove members | All `403` with the manager-only messages |
| Resources reached through another project's path | Rui, through the path of "App": a task and a comment of a project he isn't a member of (read, edit, delete, comment, assign) | All `404` ("Task not found." / "Comment not found."), nothing changed |
| Comment through another task's path | Ana: her comment through a different task of the same project | `404` "Comment not found." |
| Only the author changes a comment (SEC11) | Rui edits and deletes Ana's comment | `403` "Only the author can change this comment." |
| Only the team manager changes a team | Rui renames, deletes, adds and removes members of Ana's team | All `403` |
| Assignee must be a member | Ana assigns Sara, who isn't a member | `422` "The assignee must be a member of the project." |
| CSRF (DoD06, SEC07) | No token; Rui's token on Ana's session | `403` "The request could not be verified. Please reload and try again." |
| Session (DoD02, SEC01) | No session, read and change | `401` "Please log in to continue." |
| State after the refused changes | Read back the project name, tasks, comments, team name and members | All unchanged |
| Emails (PRI03, PRI04) | `GET /api/teams` as Sara; eligible assignees as Ana | No email in either |
| Password hashes (SEC05) | Searched every answer of the run for "password", BCrypt prefixes | None |
| Secrets in the repository and history | `.env` never committed; every version of `.env.example` holds only `change-me-…` values; patterns for private keys, cloud and API tokens, connection strings with credentials, across all commits | Nothing found. Test passwords are fictitious and only in `src/test` (SEC10). |
| Static analysis | Semgrep 1.157.0, `--metrics=off`, rule sets `p/java`, `p/security-audit`, `p/typescript`, `p/react`, `p/secrets`, `p/owasp-top-ten` (239 rules, 107 files) | 0 findings |
| Long text | A 200 000-character comment | `422` "Comment must be at most 2000 characters." |

**Notes on the run:**
- **Environment:** the access checks ran against a throw-away PostgreSQL in Docker and the JAR at `c5a0943`, with the demo accounts and passwords made up for the run. The developer's database was not used, and the container and process were removed afterwards.
- **Semgrep:** the first run asked for `p/spring`, which no longer exists in the Semgrep registry, so that run scanned nothing. It was repeated without it.
- **A mistake in the access script:** one line of the script expected a refusal when Rui, the manager of "App", removed Ana from "App". That removal is allowed (US09), so the "failure" was the script's, not the application's.

## Not verified

- **Simultaneous requests:** the access tests ran one request at a time. Races between access checks and changes were covered only by the existing tests (PD05, R2).
- **Session fixation and expiry:** session fixation, session expiry (PD02) and log-out invalidation were not retested. They are covered by `SessionApiTest`.
- **Rate limiting:** brute-force protection on log-in is not in the requirements, and it wasn't tested.
- **HTTPS (SEC08):** it only applies to a deployment, which isn't decided yet (PD13).

## Decisions

Taken by the developer on 2026-11-14:

| Finding | Decision |
|---|---|
| F1 | Fix. A NUL character is refused like an unreadable body: `400` "The request body is not valid." (DoD07), option (a). |
| F2 | Fix as proposed: only a foreign-key violation (SQL state `23503`) means "not found". |
| F3 | Add the missing test. |
| F4 | Fix, option (b): a search containing NUL matches no project (empty list), and the totals stay as they are (D14). |
| F5 | Fix: upgrade Node to v24.22.3. |
| F6 | Fix: upgrade `frontend-maven-plugin` to 2.0.2. |

## Fixes

| Finding | Change | Regression test | Without the fix | With the fix |
|---|---|---|---|---|
| F1 | `web/internal/JsonConfig.java`: every JSON string goes through a reader that refuses NUL. The refusal reaches `ApiExceptionHandler` as an unreadable body, so every endpoint gets `400` "The request body is not valid." from one place, without any change to the modules. | `ReviewRegressionTest.textWithANulCharacterIsRefusedAsAnUnreadableBodyAndNothingIsStored`: NUL in the project name (create, edit), task title, description and title edit, comment text, team name (create, edit), member email; checks each answer and that no row in `project`, `task`, `task_comment`, `team` or `project_member` changed | Fails: `POST /api/projects` got `500` | Passes |
| F2 | `tasks/internal/Problems.java`: new `isMissingReference` (SQL state `23503` anywhere in the causes). The three catches in `TasksController`, `CommentsController` and `TaskAssignees` use it and rethrow anything else. | `MissingReferenceTest` (3 unit tests): `23503`, directly or deeper in the causes, means a missing reference; `22021`, `23505`, `23514`, `22001` and no cause don't | Fails when the method answers "yes" to everything, as the old catches did: SQL state `22021` | Passes |
| F3 | No production change. | `ReviewRegressionTest.aTeamDeletedJustBeforeItsAssignmentIsSavedIsNotFound`: the team is deleted in a held transaction while it is being assigned; expects `422` "Team not found." and no assignee row | Fails when the catch in `TaskAssignees` is removed | Passes |

| F4 | `projects/internal/JdbcProjects.java` (`of`): a search text containing NUL returns no project without querying the database. No project name can contain NUL once F1 is fixed. | `ReviewRegressionTest.aSearchWithANulCharacterMatchesNoProjectAndKeepsTheTotals`: `?name=%00` and `?name=Web%00site` give `200`, no project, and the totals unchanged | Fails: `?name=%00` got `500` | Passes |

**Behaviour change to note:** F1's fix applies to every JSON body, log-in included. A log-in whose email or password contains NUL now gets `400` "The request body is not valid." instead of `401`, and still reveals nothing about accounts (SEC04). This is now tested: `ReviewRegressionTest.aLogInWithANulCharacterIsRefusedAsAnUnreadableBodyWithoutASession` checks NUL in the email and in the password; both get `400` with that message and no session cookie.

**Build:** `./mvnw verify` gives BUILD SUCCESS in 132 s. There are 207 back-end tests (200 before, plus these 7) and 46 browser application tests, and the end-to-end test passes too.

## Checks run after the fixes

These were first listed as not verified; the developer asked for them to be checked.

| Check | How | Result |
|---|---|---|
| Log-in with NUL | Regression test (see above) | `400` "The request body is not valid.", no session |
| Cross-site scripting in the browser application | In the throw-away environment, stored `<script>`, `<img onerror>` and `<svg onload>` payloads in a project's name and description, a task's title and description, a comment and a team's name (assigned to the task). Then opened the dashboard, board, task details, members and teams screens in Chromium (Playwright), waiting for each payload to be shown. | No script ran (no dialog, `window.__xss` never set), no `<img src=x>`, `<svg onload>` or injected `<script>` in the page; every payload is shown as literal text, including in the breadcrumb and the assignees |
| Known vulnerabilities, browser application | `npm audit` in `frontend/` | 0 (info, low, moderate, high, critical) |
| Known vulnerabilities, Java | The 155 resolved Maven dependencies (`mvnw dependency:list`, every scope) checked against the public OSV database (osv.dev): only public library coordinates were sent. The query was first shown to work: `log4j-core` 2.14.1 gives 8 known vulnerabilities. | 0 dependencies with known vulnerabilities |
| Security headers | `GET /`, `/index.html` and `/api/session` | `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Cache-Control: no-store` on all; no `Content-Security-Policy` |

**Observation, not a finding:** there is no `Content-Security-Policy`. No requirement asks for one, and the XSS check above found React's escaping sufficient, so it is not reported as a problem. A CSP would be extra protection to consider with the deployment (PD13).

## Build tools

Checked after the fixes, at the developer's request. **None of these tools is part of the JAR:** they run only on the machine that builds the project. Their inputs are the project's own files, and downloads from `nodejs.org`, `registry.npmjs.org` and Maven Central over HTTPS.

**What was checked:**
- **Maven plugins:** `./mvnw dependency:resolve-plugins` lists the 10 plugins of the build and their own libraries. These 218 artefacts, plus Maven 3.9.11 (the wrapper's version), were checked against OSV.
- **npm:** npm 11.12.1, the one the build installs, was checked against OSV (ecosystem npm).
- **Node:** v24.15.0 was compared with the releases published at `nodejs.org/dist/index.json`, which mark the security releases.

| Result | Detail |
|---|---|
| Maven 3.9.11 | No known vulnerability |
| npm 11.12.1 | No known vulnerability |
| `maven-site-plugin` 3.12.1 | 18 artefacts with known vulnerabilities (Jetty 9.4, Velocity 1.7, dom4j 1.1, jsoup 1.10, Guava 16, …). **Not used:** it runs only with `mvn site`, never in `verify`; Maven lists it because it is bound to the site lifecycle by default. No action. |
| `maven-compiler-plugin`, `maven-resources-plugin`, `maven-clean-plugin`, `maven-install-plugin`, `maven-deploy-plugin`, `spring-boot-maven-plugin` | `plexus-utils` 3.5.1–4.0.2 (GHSA-6fmv-xxpf-w3cw, directory traversal in `extractFile`), `commons-io` 2.11.0 (GHSA-78wr-2p64-hpwj, `XmlStreamReader` on untrusted input), `commons-lang3` 3.16.0 (GHSA-j288-q9x7-2f5v, recursion on long input). **Accepted, no action:** none of these plugins extracts archives or reads untrusted XML here. Their versions come from Spring Boot 4.1.2's plugin management; the newer releases are betas (e.g. compiler 4.0.0-beta-5). They will be updated with Spring Boot. |
| `frontend-maven-plugin` 1.15.4 | See F6 |
| Node v24.15.0 | See F5 |

### F5 — The build installs Node v24.15.0, four security releases behind

- **Status:** confirmed. **Severity:** medium.
- **File:** `pom.xml` (`<node.version>v24.15.0</node.version>`), which `frontend-maven-plugin` downloads into `target/node`.
- **Evidence:**
  - v24.15.0 was released on 2026-04-15, and the 24 line is now at v24.22.3 (2026-11-10).
  - Of the 11 releases since, 4 are marked as security releases: v24.17.0, v24.18.1, v24.22.1 and v24.22.3.
- **Impact:** this Node runs every browser-application tool of the build: Vite, Vitest, Playwright and `frontend/e2e/server.mjs`. `npm run dev` also uses it to serve the development server, if run with the same Node. The fixed vulnerabilities apply to that machine, not to the deployed application.
- **Proposed fix:** set `node.version` to `v24.22.3`. This is a dependency upgrade, so it needs the developer's approval (AGENTS.md §6). The README asks for "Node 24" and stays correct.
- **Proposed regression check:** `./mvnw verify`, browser tests and end-to-end test included. A pinned version can't be tested to stay current, so a test can't guard this.

### F6 — `frontend-maven-plugin` 1.15.4 brings libraries with known vulnerabilities

- **Status:** confirmed. **Severity:** low.
- **File:** `pom.xml` (`<frontend-maven-plugin.version>1.15.4</frontend-maven-plugin.version>`).
- **Evidence:** the plugin brings these libraries with known vulnerabilities:
  - `jackson-core` 2.13.0 and `jackson-databind` 2.13.4.2 (14 advisories);
  - `commons-compress` 1.21 (GHSA-4265-ccf5-phj5, GHSA-4g9r-vxhx-9pgx);
  - `commons-io` 2.11.0;
  - `plexus-utils` 3.5.1 (GHSA-6fmv-xxpf-w3cw).

  The latest release, 2.0.2, uses `commons-compress` 1.28.0, `commons-io` 2.21.0 and `plexus-utils` 4.0.3, which have none, and Jackson 2.21. That Jackson still has 7 advisories.
- **Impact:** the plugin unpacks the Node archive and reads JSON, but only files downloaded from `nodejs.org` over HTTPS and the project's own `package.json`, so the vulnerable paths get no untrusted input. The risk is low.
- **Proposed fix:** upgrade to 2.0.2. It is a new major version, so the configuration in `pom.xml` (`installDirectory`, the `npm` goals, `skip`) may need changes. It needs the developer's approval (AGENTS.md §6).
- **Proposed regression check:** `./mvnw verify`, including `-Dskip.npm` and `-Dskip.e2e`, so that every npm step still runs and can still be skipped.

### Fixing F5 and F6

**Change:** in `pom.xml`, `node.version` went from `v24.15.0` to `v24.22.3`, and `frontend-maven-plugin.version` from `1.15.4` to `2.0.2`. No other configuration had to change.

| Check | Result |
|---|---|
| `./mvnw verify` | **BUILD SUCCESS** in 128 s. The plugin 2.0.2 installed Node v24.22.3 and npm 11.19.1. There are 207 back-end tests and 46 browser application tests, and the end-to-end test passes. |
| npm 11.19.1 against OSV | No known vulnerability |
| `frontend-maven-plugin` 2.0.2 and its libraries against OSV | The plugin itself, `commons-compress` 1.28.0, `commons-io` 2.21.0, `plexus-utils` 4.0.3: none. Its Jackson 2.21 still has 7 advisories (accepted, for the reasons in F6). |
| Skip flags, without `-DskipTests` and with Java tests limited to `CookieDefaultsTest` | No flag: every npm step runs, the end-to-end test included. `-Dskip.e2e`: only `npm-e2e` is skipped. `-Dskip.npm -Dskip.installnodenpm`: the Node installation and every npm step are skipped. |

**Problems met on the way, none caused by the change:**
- **`./mvnw clean verify` couldn't delete `target/`.** The IDE's Java extension rebuilt `target/classes` while Maven was deleting it. The build was run without `clean`.
- **155 back-end tests failed once in the first `verify`.** The log-in gave no `MATRIZ_SESSION` cookie. The same tests passed when run alone, then all together (207 of 207), then in the full `verify`. The likely cause is the same IDE rebuild: a Spring context started without `application.yaml` names the cookie `SESSION`. This was classified as a limitation of the environment, not a defect.
- **An invalid first check of the skip flags.** The shell passed the two flags as one argument, and `-DskipTests` skipped the test steps by itself. It was repeated correctly, as in the table above. The same mistake had affected a check recorded in exercise 4, which was corrected in [verification.md](../verification.md), section 12.
- **A warning from npm 11.19.1 during `npm ci`:** "1 package has install scripts not yet covered by allowScripts: fsevents@2.3.3". It is a notice from the newer npm, which no longer runs install scripts that haven't been approved. The build still passes. `fsevents` is an optional macOS file-watching package that Vite uses, so nothing breaks without its script.

