## Role
You are a senior DevOps engineer, specialised in GitHub Actions for Java and Node projects. You prefer simple, reproducible pipelines that run the same commands a developer runs locally. You treat the workflow itself as sensitive code: minimal permissions, and actions pinned to exact versions.

## Goal
Add a continuous integration (CI) workflow that runs on every push to `main`, on every pull request to `main`, and on demand (`workflow_dispatch`). It does three things:
1. **Build and tests:** runs the full `./mvnw verify`: back-end tests (Testcontainers), browser application tests, `ModularityTest` and the end-to-end test.
2. **Static analysis:** runs **Semgrep** and **CodeQL** (SAST) on the code, and **OSV-Scanner** (SCA) on the Maven and npm dependencies.
3. **The result:**
   - the workflow fails when a test fails or when a static analysis tool reports a finding;
   - the test reports (Surefire, and Playwright's trace and screenshot) are kept as artefacts only when something fails.

Continuous deployment (CD) is out of scope for now (Lesson 05; the platform, PD13, is still pending).

## Context
- **Project:** Matriz, on GitHub at `vitorhsbarros/matriz`, a **public** repository, so CodeQL's results can be shown in GitHub's code scanning.
- **Build:**
  - `./mvnw verify` builds and tests everything (see `README.md` and AGENTS.md §10). The last local run gave BUILD SUCCESS: 213 back-end tests, 46 browser application tests and 1 end-to-end test.
  - It needs Java 21 and Docker (for Testcontainers and the end-to-end test's throw-away PostgreSQL), and port 8095 free.
  - Node v24.22.3 and Chromium are installed by the build itself (`frontend-maven-plugin`, `npm run e2e`).
- **No secrets:** the tests use fictitious passwords, and the end-to-end test makes up its own. The workflow needs no GitHub secrets.
- **Static analysis:** the code and security review ran Semgrep with `p/java`, `p/security-audit`, `p/typescript`, `p/react`, `p/secrets` and `p/owasp-top-ten`, with 0 findings (`docs/quality-security-review.md`). The same rule sets are used here.
- **What exists:** there is no CI in the project yet, and no `.github/` folder.

## Constraints
- **Tool (approved by the developer):** GitHub Actions joins the stack. It is recorded in `docs/architecture.md`, section 5.
- **Workflow security:**
  - minimal `permissions`: `contents: read` for the workflow, and `security-events: write` only for the CodeQL job;
  - every action is pinned to an exact commit SHA, with the version in a comment;
  - no `pull_request_target`;
  - no secrets.
- **Files:**
  - one workflow file, `.github/workflows/ci.yml`, with four jobs: `build`, `sast-semgrep`, `sast-codeql` and `sca`;
  - the only other changes are:
    - the line in `docs/architecture.md`, section 5;
    - one section in `docs/verification.md` recording the first run on GitHub. It is written once, and from then on the GitHub Actions history is the evidence for each run. The section is updated only when the CI itself changes in a relevant way.
  - no change to the application, the tests or the build configuration.
- **Semgrep:** run with `--metrics=off` and the rule sets listed in the Context.
- **Ambiguity:** when something is ambiguous or not specified, stop and ask.
- **Publishing:**
  - pushing the workflow makes it run on GitHub's servers (an external service, AGENTS.md §6), so ask for confirmation before every push;
  - never push only to test the workflow without asking.
- **Language:** the workflow, its comments and the documentation are in English (C05).

## Task
1. **Check the starting point:** confirm there is no `.github/`, and note the state of `main`.
2. **Choose the actions' versions:** pick exact versions of every action used (e.g. `actions/checkout`, `actions/setup-java`, `actions/upload-artifact`, `github/codeql-action`, OSV-Scanner's action), and the commit SHA of each. None released less than 14 days ago.
3. **Write `.github/workflows/ci.yml`** with four jobs:
   - **`build`:**
     - Java 21 with the Maven cache, then `./mvnw verify`;
     - on failure, upload the Surefire reports and `frontend/test-results/` as artefacts.
   - **`sast-semgrep`:** Semgrep with `--metrics=off` and the review's rule sets; fails on any finding.
   - **`sast-codeql`:** CodeQL for Java and JavaScript/TypeScript, with the results published to code scanning. A further step fails the job when the analysis reports any alert.
   - **`sca`:** OSV-Scanner on `pom.xml` and `frontend/package-lock.json`; fails on any known vulnerability.
4. **Validate locally before any push:**
   - with `actionlint`, if available;
   - by checking each command against what runs locally;
   - by running Semgrep and OSV-Scanner locally, if available, so a first red run is not a surprise.
5. **Show the file to the developer** and ask for confirmation before pushing.
6. **After the push:**
   - follow the run on GitHub (`gh run watch`);
   - if a job fails, classify the cause (a problem in the workflow, a real finding, or a limitation of the runner) and report it before changing anything;
   - once the run is green, add the section to `docs/verification.md`: the run's link and commit, each job's result and duration, any difference from the local run, and a line saying that later runs are recorded in the GitHub Actions history.

## Validation
Before reporting, run these checks and give each one as passed or failed:
1. **Before the push:**
   - `actionlint` reports no errors, or the file is reviewed by hand if it isn't available;
   - every action is pinned to a commit SHA;
   - `permissions` are minimal, with no secrets and no `pull_request_target`;
   - Semgrep and OSV-Scanner were run locally, and the result the CI should give is known.
2. **After the push:**
   - the four jobs are green on GitHub;
   - CodeQL's results appear under Security → Code scanning.
3. **The gates really fail:**
   - after the developer confirms, push a throw-away branch with a commit that breaks one test, and run the workflow on it with `workflow_dispatch`;
   - `build` must fail, and the test reports must be uploaded as artefacts;
   - then delete the branch, locally and on GitHub.
4. **No other change:** `git diff` shows changes only in `.github/workflows/ci.yml`, `docs/architecture.md` and `docs/verification.md`.
5. **Final report:** follow AGENTS.md §9 (changes, assumptions, checks, not checked), with the results of checks 1 to 4.
