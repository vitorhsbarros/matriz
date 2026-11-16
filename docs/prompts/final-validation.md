## Role

You are a senior release reviewer, independent of the team. You validate the delivery from scratch, as a new developer would: you follow the README literally, trust only what you can reproduce, and back every conclusion with evidence. You report problems plainly, including your own earlier mistakes.

## Goal

Do the final validation of Matriz's readiness for delivery (Lab 05, final exercise), and produce a short validation report that states:
- the requirements met;
- the problems found, with their fixes;
- the limitations still open.

The validation covers:
1. running the application from the README's instructions;
2. the state of the containers;
3. the essential features;
4. the CI/CD pipeline's results;
5. the logs, for warnings and errors;
6. no credentials in the versioned files;
7. a final review of the documentation, the Docker configuration, the pipeline and the test results.

## Context

- **Read first:** `README.md`, `AGENTS.md`, `CHANGELOG.md`, `docs/release-notes/`, `docs/architecture.md`, `docs/delivery-plan.md` (§10), `docs/verification.md` (§14 to §20), `docs/quality-security-review.md`, `docs/test-plan.md`, `docs/deployment-options.md`, the `Dockerfile`, `.dockerignore`, `compose.yaml`, `.env.example`, `.github/workflows/ci.yml` and `scripts/image-smoke-test.sh`.
- **The current version** is `v1.1.1` on `main`, published as `ghcr.io/vitorhsbarros/matriz:1.1.1` (also `latest`) with a provenance attestation. Earlier releases: `v1.0.0` (the MVP), `v1.0.1` (Lab 04), `v1.1.0-rc.1` and `v1.1.0` (Lab 05).
- **What each lab delivered** is recorded in `docs/verification.md`, `docs/quality-security-review.md` ("Delivery") and `docs/delivery-plan.md` ("Delivery").
- **Known open points:** PD13 (the deployment platform, compared in `docs/deployment-options.md`, not decided), the image is amd64 only, and the Java runtime isn't covered by either scan.
- **The developer's machine:**
  - port 8080 is taken by another program, and 8081 by a local JAR of an older build;
  - the developer's `postgres` container (Compose project `matriz`) uses port 5432 and holds data that must be kept;
  - the machine is arm64, while the published image is amd64.

## Constraints

- **Validate, don't change:**
  - report every problem with its evidence, and propose a fix;
  - apply a fix only after the developer approves it;
  - documentation-only fixes can be batched into one approval.
- **No invented results:** every "met" in the report points to a command run during this check, or to a CI run, with its result. What wasn't run is listed as not run.
- **The clean copy:**
  - clone the repository from GitHub into a new folder in the scratchpad, whose name differs from `matriz`, so Compose uses another project and another volume;
  - make its `.env` from `.env.example` as the README says, keeping the fictitious example values;
  - never touch the developer's working copy, `.env` or data.
- **Ports:**
  - stop the developer's `postgres` during the check (`docker compose stop`, never `down -v`), and start it again at the end;
  - leave the JAR on 8081 alone;
  - use `MATRIZ_APP_PORT=8082` where the README's default port 8080 is taken.
- **Secrets:** never print a secret found. Report the file, line and kind only.
- **Scope:**
  - don't make PD13;
  - no push, tag, release or publication without the developer's approval;
  - no new tools: only those already used (Docker, Compose, Maven wrapper, `gh`, `jq`, `curl`, Semgrep's image).
- **Report:** `docs/final-validation.md`, short (about one page), in English, linked from the README's documentation table. Commits start with "Lab 05, exercise 6:".
- **Language:** files and commit messages in English, the conversation in the developer's language.

## Task

1. **From the README:**
   - clone the repository into the scratchpad, at `main`;
   - follow "Running the application" step by step: the configuration, `docker compose up -d --wait`, `./mvnw verify`, `java -jar target/matriz.jar`, and the health and log-in checks;
   - then "Running with Docker": `build`, `up -d --wait`, the health URLs, the logs, `scripts/image-smoke-test.sh`, `down`;
   - note every step that fails, is unclear, or differs from what the README says.
2. **The containers:** `docker compose ps` (state and health), the ports bound to `127.0.0.1`, the user, the read-only file system, and the volume kept after `down`.
3. **Essential features**, with the demo accounts and `curl`:
   - log-in and log-out;
   - the dashboard;
   - creating, moving, editing and deleting a task;
   - a comment;
   - the access rules: Rui can't edit *Website*, and Sara gets "Project not found.".

   The end-to-end test in `./mvnw verify` covers the browser.
4. **CI/CD:** with `gh`, the result of the latest runs on `main` and of the `v1.1.1` release run, the annotations (warnings) of those runs, the published package and its attestation (`gh attestation verify`).
5. **Logs:**
   - the application's logs (JAR and container), counted by level, with every `WARN` and `ERROR` explained;
   - the build's warnings in `./mvnw verify`.
6. **Credentials:**
   - Semgrep `p/secrets` (the image and rules the CI uses) over the clean copy;
   - pattern searches over `git ls-files` and over the whole Git history (passwords, tokens, private keys, connection strings with a password);
   - confirm `.env` was never committed, and that `.env.example` has only fictitious values.
7. **Final review:** check that the documentation, the Docker configuration, the pipeline and the recorded test results agree with each other and with what was observed in steps 1 to 6.
8. **Restore:** remove the clean copy's containers, volume and image, delete the copy, and start the developer's `postgres` again.
9. **Report:** write `docs/final-validation.md`, with:
   - the requirements met, each with its evidence;
   - the problems found, with the proposed or applied fix;
   - the limitations still open;
   - the verdict (ready, ready with reservations, or not ready).

   Link it from the README.
10. Ask the developer to approve the proposed fixes, the commits and the push. Wait.

## Validation

1. **Coverage:** each of the seven validation points in Goal has a result in the report, with evidence or marked as not run.
2. **README:** every step from step 1 was run as written, in the clean copy, or the report says why not.
3. **Evidence:** every number in the report (tests, runs, digests, counts) comes from a command or run of this check, not copied from earlier records.
4. **Secrets:** the report names no secret value, and the credentials check covered both the files and the history.
5. **Restored:** the developer's `postgres` is running with its data, the clean copy and its containers, volume and image are gone, and the working copy has only `docs/final-validation.md`, the README link and this prompt changed.
6. **Verdict:** it follows from the findings, and every reservation is listed.

If a check fails, fix it and try again, at most twice. Then stop and ask.
