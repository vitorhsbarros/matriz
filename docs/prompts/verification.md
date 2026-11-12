## Role

You are a QA engineer experienced in acceptance testing from Gherkin criteria and end-to-end browser testing. You accept only what you observed, and you record evidence for every claim.

## Goal

Demonstrate that the application meets its requirements and keeps its data, and record it in `docs/verification.md`: for each scenario, the expected result, the observed result and the evidence. Verification checks done by hand are kept apart from automated tests, and what couldn't be run is named.

| Case | Scenario | Result to confirm |
|---|---|---|
| 01 | Access to the application | Valid credentials let the user in; invalid credentials and unauthenticated requests are handled. |
| 02 | Project management | Create, search, edit and delete work according to the permissions. |
| 03 | Task persistence | A task appears without a reload and is still there after the back end restarts. |
| 04 | Data validation | Required fields that are empty or only spaces, and invalid values, are refused. |
| 05 | Status and priority | Editing updates the task, the filters and the counts consistently. |
| 06 | Assignees | Adding and removing work; the same assignment is never duplicated. |
| 07 | Comments | The text, the authenticated author and the date are saved and shown. |
| 08 | Authorization in the API | Another user can't read or change resources without permission, even by changing the identifiers in the request. |
| 09 | Deleting and errors | Deleting respects dependent data; API errors never show a success message or lose the form's data. |

- At least three relevant automated tests are created and run: one validation rule, one authorization scenario, and one creation followed by a check of what was stored.
- When a bug turns up, it is reproduced, its evidence collected and its cause found, and it gets a limited fix with a regression test.
- **Expected outcome:** checks recorded, tests run, and known problems identified.

## Context

- **Project:** Matriz, a simplified Jira-like project and task management application. Its MVP is complete (increments I0–I6, the 23 user stories): a modular monolith in Java 21 and Spring Boot 4 with PostgreSQL, and a React + TypeScript browser application served from the same JAR.
- **Existing tests:**
  - **back end:** real HTTP against PostgreSQL with Testcontainers;
  - **browser application:** Vitest and Testing Library, run by `./mvnw verify`;
  - **browser checks:** Playwright (Chromium and WebKit) is installed in the session's scratchpad, not as a project dependency.
- **Demo accounts** (made up, PRI05):
  - Ana manages *Website* and is a member of *App*;
  - Rui is a member of *Website* and manages *App*;
  - Sara isn't a member of *Website*.

  Their passwords are in the local `.env`.
- **Environment:**
  - PostgreSQL runs in `docker compose`;
  - the application runs on port 8081 (`SERVER_PORT=8081`), because port 8080 is in use on this machine;
  - the local `.env` sets `MATRIZ_COOKIE_SECURE=false`, so Safari keeps the session cookie on `http://localhost`.
- **Read first:**
  - `AGENTS.md`;
  - `docs/user-stories.md` (the stories, DoD01–DoD07, D01–D14);
  - `docs/constraints.md` (SEC, PRI);
  - `docs/api-contract.md`;
  - `docs/implementation-plan.md`.

## Constraints

- **No code changes,** except:
  - the three new tests;
  - bug fixes, which are made only after the user approves the diagnosis, each with a regression test.
- **Real evidence only:** nothing is marked Pass without being observed. Each row says whether the check was done by hand or is an automated test.
- **No secrets in the document:** no passwords, session identifiers or CSRF tokens. Test data is made up (PRI05).
- **Clean up:** data created in the local database for the checks done by hand is deleted at the end, and the document says what was created and deleted.
- **No new dependencies.**
- **Screenshots:** about ten key screenshots go in `docs/verification/` and are referenced from the document.
- The document is in English.

## Task

1. **Prepare.** Read the documents listed in Context. For each case 01–09, decide the steps, the expected result (from the acceptance criteria) and the evidence to collect. Present this plan and wait for approval.
2. **New tests.** Create and run three automated tests, mapped to:
   - case 04: a validation rule;
   - case 08: an authorization scenario, with identifiers changed in the request;
   - case 03: a creation followed by a check of what was stored in the database.
3. **Full automated run.** Run `./mvnw verify`, and record the command, the totals and the duration.
4. **Manual checks in a real browser,** against the real API and database, with the demo accounts. For data persistence:
   1. a page reload;
   2. a back-end restart;
   3. a database restart (`docker compose down`, then `up`);
   4. a check that the seed doesn't change existing data.
5. **Bugs.** If a bug turns up, stop and present the diagnosis, following the support prompt:
   - reproduce it, with the expected and observed results and the evidence;
   - locate the code involved;
   - separate hypotheses from conclusions;
   - say how to confirm the cause;
   - propose a limited fix.

   Fix it only after approval, with a regression test.
6. **Write `docs/verification.md`:**
   - the environment and versions;
   - the commands run;
   - one table per case, with the step, the expected result, the observed result, the evidence, the kind (manual or automated) and the result (Pass, Fail or Not verified);
   - the known problems and limitations.
7. **Update** README.md (the Documentation section) and `docs/implementation-plan.md`, and report as in AGENTS.md §9. Don't commit.

## Validation

1. **Cases:** each of the 9 cases has at least one row, with the expected result, the observed result and the evidence.
2. **Traceable evidence:** every piece of evidence points to something that can be checked again: a test name, a command with its output, a screenshot in `docs/verification/`, or a database query.
3. **Tests:** the three new tests pass, and `./mvnw verify` passes.
4. **Secrets:** a search confirms that the document and the screenshots contain no password, session identifier or token.
5. **Consistency:** the totals of Pass, Fail and Not verified match the tables.
6. **Clean-up:** the data created for the checks done by hand has been deleted.
