## Role
You are a senior test engineer, specialised in end-to-end testing with Playwright. You test the running application in a real browser, as a user would, finding elements by their visible text and their role on the screen. You take expected results from the user stories and the wireframe, and you read the code only to learn how to start the application.

## Goal
Validate one functional user journey through the interface, with an automated end-to-end test of this flow:
**log in → open a project → create a task → check the task appears on the project's board**.

The result is one of these:
- **an end-to-end test that can be reproduced:** it runs again and again without depending on data or earlier runs, and its assertions prove the result on the screen;
- **a documented technical limitation, with evidence**, if such a test can't be made to work.

When the test fails, the cause is classified as one of three: a defect in the application, a problem with the test, or a limitation of the environment.

## Context
- **Project:** Matriz, a project and task management application. The back end is Spring Boot, packaged with the React browser application as one JAR (`./mvnw verify` builds `target/matriz-0.1.0-SNAPSHOT.jar`). It stores its data in PostgreSQL.
- **Sources of the expected results:**
  - `docs/user-stories.md`: US02 (log in), US04 (home dashboard), US05 (create a task) and US06 (the project's task board), for the messages and behaviour;
  - `docs/ui/README.md`: what each screen shows;
  - `docs/test-plan.md`: scenario TP10.
- **How the application runs for the test:** from the JAR, against a throw-away PostgreSQL in Docker.
  - The configuration comes from environment variables, as in `README.md` (`MATRIZ_DB_URL`, `MATRIZ_DEMO_SEED`, …).
  - On `http://localhost`, `MATRIZ_COOKIE_SECURE=false` is needed, or some browsers keep no session cookie.
- **Test user:** the demo account Ana (`ana@example.com`), created by the seed when `MATRIZ_DEMO_SEED=true`. Her password comes from `MATRIZ_DEMO_PASSWORD_ANA`, set only in the test's environment and never written in the repository.
- **Test data:** every run creates its own project, with a unique name.
- **What exists:** there is no Playwright and no end-to-end test in the project yet. The browser application's tests are Vitest tests in a simulated browser (jsdom).
- **Production code:** may be read only to learn how to start the application, never to decide an expected result.

## Constraints
- **Tool (approved by the developer):**
  - `@playwright/test` as a devDependency of `frontend/`, with the tests in `frontend/e2e/`;
  - Chromium only.
- **Build:** the end-to-end test is part of `./mvnw verify`. When it fails, the build fails.
- **Waiting:** no fixed waits (`waitForTimeout`, sleeps). Use only Playwright's automatic waiting and web-first assertions (`expect(...).toBeVisible()`, `toHaveText()`, …).
- **Selectors:**
  - find elements by role, label and visible text (`getByRole`, `getByLabel`, `getByText`), not by CSS classes or the page structure;
  - production code stays untouched: no `data-testid` added. If an element can't be found in a stable way, stop and ask.
- **Coverage isn't proof:** the assertions must check what the user stories say the user sees, not just that the page loaded or that the code ran.
- **Repeatable:** the test depends neither on data left by earlier runs nor on the order the tests run in.
- **Diagnostics:** traces and screenshots are kept only when a test fails (`retain-on-failure`, `only-on-failure`) and looked at only to diagnose a failure.
- **Names and comments:** each test name says plainly what it checks. No specification or test-plan IDs (such as US05 or TP10) appear in test names or comments.
- **Secrets:** no password is written in the repository. It comes from the environment (SEC10, AGENTS.md §7).
- **Ambiguity:** when something is ambiguous or not specified, stop and ask.
- **Language:** code and names in English (C05).

## Task
1. **Plan first.** Present a short plan and wait for the developer's approval (AGENTS.md §4). The plan says:
   - how `./mvnw verify` starts a throw-away PostgreSQL and the JAR, and stops them;
   - how Chromium is installed;
   - which files change, and how the result is checked.
2. **Set up Playwright.** Install `@playwright/test` in `frontend/` and configure it:
   - Chromium only;
   - the application's address;
   - traces and screenshots only on failure.
3. **Prepare the test data.** Before the journey, create, through the API and as Ana, a project with a unique name.
4. **Write the journey test.**
   - Log in as Ana through the log-in screen.
   - Open the test's project from the home dashboard.
   - Create a task with a unique title.
   - Check the task appears on the board, in the column "To do", as "Unassigned".
5. **Hook the test into `./mvnw verify`.**
6. **Run the test.** If it fails, classify the cause as a defect in the application, a problem with the test, or a limitation of the environment, and act on it:
   - correct the test if the test is wrong;
   - record a defect, without changing the code, if the application is wrong;
   - document the limitation, with evidence, if the environment is the cause.
7. **Record the results.**
   - Add a new section to `docs/verification.md` with the commands, the date, the result, and any correction, defect or limitation.
   - Update the commands in `README.md` and in AGENTS.md §10.

## Validation
Before reporting, run these checks and give each one as passed or failed:
1. **Repeatable:** the test passes twice in a row, and also with a brand-new database.
2. **Full build:** `./mvnw verify` passes, and its log shows the end-to-end test ran.
3. **The test checks the result:** make a temporary change to the test's expectation (e.g. the expected column or title) and confirm the test fails, with a trace kept. Then revert it.
4. **Test code review:**
   - a search finds no `waitForTimeout` or other fixed waits;
   - selectors use only role, label and text;
   - no password is in the repository.
5. **No production change:** `git diff` shows no change under `src/main` or `frontend/src`.
6. **Sources:** every message and status name the test expects ("To do", "Unassigned", …) is in the user stories, character for character.
7. **Final report:** follow AGENTS.md §9 (changes, assumptions, checks, not checked), with the results of checks 1 to 6.
