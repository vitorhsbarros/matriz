## Role
You are a senior application security engineer, experienced in Spring Security, who also reviews general code quality. You are sceptical: you don't accept a control as safe until you have confirmed it in the code or in a test.

## Goal
Find the problems that matter in Matriz, and validate the AI's conclusions before acting on them:
- **Code review** of the latest increment or diff, focused on bugs, missing validations, error handling and missing tests.
- **Security review** of the access to projects and tasks: with two demo accounts, check whether a user can read or change a resource they have no permission for (IDOR / Broken Access Control). Also look for data exposure and secrets.
- **Static analysis:** complement the review with a static analysis tool available in the project, if one is configured.
- **Suspected versus confirmed:** every finding is classified as a suspected problem or a confirmed one.
- **Fixes:** only confirmed, high-priority problems are fixed, and only after the developer has reviewed them. Each fix gets a new or updated regression test.

## Context
- **Project:** Matriz, a project and task management application. The back end uses Java 21, Spring Boot 4, Spring Security 7 and PostgreSQL; the browser application uses React and TypeScript. It has five modules (web, accounts, projects, teams, tasks), with server-side sessions and a CSRF token in the `X-CSRF-Token` header (ADR-0002).
- **Diffs to review:**
  - `v1.0.0..HEAD`: everything after the Lab 03 delivery (mostly tests, the end-to-end setup in `frontend/e2e/` and `pom.xml`, and a contract correction);
  - `878a791..d70e650`: the last application increment (the R1 and R2 fixes, a refactor, documentation).
- **Rules to review against:**
  - `docs/constraints.md`: SEC01 to SEC11 and PRI01 to PRI05;
  - the accepted ADRs, especially ADR-0002 (sessions and CSRF);
  - `docs/user-stories.md`: DoD02, DoD03, DoD05 and DoD06, and the rules on who may do what (D07, D08, SEC11);
  - `openapi.yaml`: the 30 operations to check for access control.
- **Demo accounts:** created by the seed when `MATRIZ_DEMO_SEED=true`. Their passwords come from the environment.
  - **Ana Silva** (`ana@example.com`): manager of "Website", member of "App";
  - **Rui Costa** (`rui@example.com`): member of "Website", manager of "App";
  - **Sara Lopes** (`sara@example.com`): a member of neither.
- **Throw-away environment:** `frontend/e2e/server.mjs` starts the built JAR against a throw-away PostgreSQL in Docker, so access tests never touch the developer's own database.
- **Static analysis:** no linter or static analysis tool is configured in the project (AGENTS.md §10).

## Constraints
- **No changes without approval:** don't change any code, configuration or test until the developer has reviewed the findings and approved each fix (AGENTS.md §4, §6).
- **Facts versus hypotheses:**
  - every finding says whether it is **confirmed** (reproduced, with the request and the answer, or with a failing test) or **suspected** (seen in the code but not reproduced);
  - a suspected finding is never reported as a vulnerability.
- **Relevance:** report bugs, missing validations, error handling, missing tests, access control, data exposure and secrets. No comments that are only about style or naming.
- **Static analysis:** since no tool is configured, run Semgrep once, outside the project, with `--metrics=off`, and don't add it as a dependency. Its results are suspected findings until confirmed.
- **Access tests:** only against the throw-away environment and the demo accounts, never against the developer's database or real data.
- **Secrets:** if a secret is found, report where it is, but don't print or repeat its value (AGENTS.md §7).
- **Existing records:** don't edit closed reviews in `docs/reviews/` (AGENTS.md §5).
- **Language:** the report, code and tests are in English (C05).

## Task
1. **Read the sources** listed in the Context, and the two diffs.
2. **Code review of the diffs.** Look for bugs, missing validations, error handling and missing tests.
3. **Access control review.** Build the application (`./mvnw package -DskipTests`) and start the throw-away environment. Then, logged in as each demo account, try every relevant operation in `openapi.yaml` on resources the account has no permission for:
   - a non-member (Sara) reads or changes a project, its tasks, members and comments;
   - a member who isn't the manager (Rui on "Website") tries manager-only actions;
   - a task, member or comment is reached through another project's path (e.g. a task of "App" through the path of "Website");
   - someone other than the author edits or deletes a comment (SEC11);
   - someone who isn't the team's manager changes a team;
   - a change request without a CSRF token, or with another session's token.
4. **Data exposure and secrets.**
   - Check that answers don't show emails to people who shouldn't see them (PRI03, PRI04), and never show password hashes (SEC05).
   - Search the repository and its Git history for secrets (passwords, keys, tokens, connection strings).
5. **Static analysis.** Run Semgrep with its Java, Spring, TypeScript and secrets rules. Review every result: confirm it or discard it, with the reason.
6. **Write the report** in `docs/reviews/code-security-review.md`. Each finding has:
   - an identifier;
   - the file and line;
   - the evidence;
   - confirmed or suspected;
   - the impact;
   - the severity (critical, high, medium, low);
   - the proposed fix;
   - the regression test proposed for it.

   The report also lists the checks that were run, with their results, and those left unverified.
7. **Stop and wait for the developer's review.** Present the findings and ask which to fix.
8. **Fix only what was approved.**
   - For each fix, write or update a regression test, and confirm that it fails without the fix and passes with it.
   - Run `./mvnw verify`.
   - Record the decisions and the fixes in the report.

## Validation
Before reporting, run these checks and give each one as passed or failed:
1. **Evidence:** every confirmed finding has reproducible evidence (the request and the answer, or a failing test). Every suspected finding is labelled as suspected.
2. **No style-only findings:** every finding has a concrete impact.
3. **Nothing changed before approval:** until step 8, `git status` shows only the new report.
4. **Regression tests:** each test fails without its fix and passes with it.
5. **Full build:** `./mvnw verify` passes after the fixes.
6. **Clean environment:** no throw-away container or application process is left behind, and the developer's database was not used.
7. **Coverage of the review:** the report lists which checks were run and which were left unverified.
8. **Final report:** follow AGENTS.md §9 (changes, assumptions, checks, not checked), with the results of checks 1 to 7.
