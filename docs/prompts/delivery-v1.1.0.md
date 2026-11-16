## Role

You are a senior technical writer and release manager for a small software team. You write documentation that a new developer can follow command by command, and you never document something that wasn't run. You release only what is complete and verified, and you say plainly what is still open.

## Goal

Close the Lab 05 delivery of Matriz, following step 5 of `docs/delivery-plan.md`:
- **Documentation:** `README.md`, `AGENTS.md` §10 and `docs/architecture.md` describe the delivery as it is now: the container image, running everything with Docker Compose, the CI `image` and `publish` jobs, the health probes and the structured logs. A reader can run the application in containers from the README alone.
- **Release:** the final release `v1.1.0` is published, and the pipeline publishes its image to `ghcr.io` as `1.1.0`, `sha-<commit>` and `latest`.

## Context

- **Read first:** `README.md`, `AGENTS.md`, `docs/architecture.md` (§5), `docs/delivery-plan.md`, `docs/verification.md` (§14 to §18), the `Dockerfile`, `compose.yaml`, `.env.example`, `scripts/image-smoke-test.sh`, `.github/workflows/ci.yml` and `docs/quality-security-review.md` ("Delivery").
- **What Lab 05 added** (recorded in `docs/verification.md`):
  - §14: a multi-stage `Dockerfile` and an allow-list `.dockerignore`. The image runs as a non-root user and installs Ubuntu security updates;
  - §15: `compose.yaml` with an `app` service in the `app` profile. `docker compose up -d --wait` still starts only PostgreSQL. `MATRIZ_APP_PORT` sets the host port, and both ports are bound to `127.0.0.1`;
  - §16: the CI `image` job, which builds the image after every check, scans it with OSV-Scanner and runs `scripts/image-smoke-test.sh`;
  - §17: the `publish` job, which pushes the tested image to `ghcr.io/vitorhsbarros/matriz` when a release is published. It was shown with the pre-release `v1.1.0-rc.1`;
  - §18: ECS JSON logs in the container (plain text outside it), and public `/actuator/health/liveness` and `/actuator/health/readiness`.
- **The docs today:**
  - **README:** it explains how to run the JAR on the host, with PostgreSQL in Compose. Its "Current status" says the version is `v1.0.1`.
  - **AGENTS.md §10:** it lists the host commands only.
  - **`architecture.md` §5:** its "Packaging" line still describes the container image as a plan.
- **Releases:** `v1.0.0`, `v1.0.1` and the pre-release `v1.1.0-rc.1`. For `v1.0.1`, the tag marked the commit that added a "Delivery" section to `docs/quality-security-review.md`. `pom.xml` stays at `0.1.0-SNAPSHOT`.
- **Known open points:**
  - PD13 (the deployment platform);
  - readiness takes 30 s to answer while the database is down (§18);
  - the image is amd64 only, and has no signatures or attestations;
  - `architecture.md` §5 says "Spring Data JPA for data access", while the code uses `JdbcClient`.
- **Environment:** on the developer's machine, port 8080 is taken by another program and port 8081 by a local JAR. Checks use `MATRIZ_APP_PORT=8082`.

## Constraints

- **Accuracy:** document only commands that are run during this task, exactly as written. Where the developer's machine differs from the documented default (port 8080), run with the override and say so in the record, not in the README.
- **Scope:**
  - change only `README.md`, `AGENTS.md` §10, `docs/architecture.md` §5, `docs/delivery-plan.md` and `docs/verification.md`;
  - no code, configuration, workflow or `pom.xml` changes. Report the version difference between the tag and `pom.xml` instead of fixing it;
  - historical records stay as they are: `docs/prompts/` (except this prompt) and the "Delivery" section of `docs/quality-security-review.md`.
- **Open points:** list the known open points in the new "Delivery" section. Don't fix them, and don't make PD13.
- **Style:** English, in the existing style of each document: short sections, tables where they help, no marketing.
- **Approval:** before the GitHub part, ask the developer once to approve:
  - the commits and the push to `main`;
  - the tag `v1.1.0` and the GitHub release, which publishes `1.1.0` and `latest` to `ghcr.io`.

  Nothing is pushed, tagged or published before that approval.
- **Commits:** messages start with "Lab 05:". The documentation in one commit; the "Delivery" section and the release record in another, which the tag `v1.1.0` points to.
- **Language:** files and commit messages in English, the conversation in the developer's language.

## Task

1. Read the sources in Context.
2. **README.md:**
   - add a section, "Running with Docker", that covers:
     - building the image;
     - `docker compose --profile app up -d --wait` and `MATRIZ_APP_PORT`;
     - the health URLs (`/actuator/health`, `/liveness`, `/readiness`);
     - the JSON logs (`docker compose logs app`);
     - stopping without losing data;
     - `scripts/image-smoke-test.sh`;
     - pulling the published image from `ghcr.io`;
   - mention the CI jobs briefly where the README describes the tests;
   - update "Current status" to `v1.1.0`, pointing to the "Delivery" section of `docs/delivery-plan.md`;
   - add `docs/delivery-plan.md` to the documentation table.
3. **AGENTS.md §10:** add the commands for the image, the Compose profile, the smoke test and the health probes.
4. **`docs/architecture.md` §5:**
   - "Packaging": the image exists (`Dockerfile`) and is published to `ghcr.io` on releases;
   - "Build and tests": add the CI `image` and `publish` jobs.
5. **Run every new command as written** (with `MATRIZ_APP_PORT=8082` where 8080 is taken), and fix the docs where they differ.
6. **`docs/delivery-plan.md`:**
   - mark step 5 as done;
   - add a "Delivery" section for `v1.1.0`: what it contains, with links to the records in `docs/verification.md`, the release and the package; what wasn't run; and the known open points.
7. Check the Lab 05 deliverables are present: `Dockerfile`, `.dockerignore`, `compose.yaml`, `.env.example`, `.github/workflows/ci.yml`, the evidence in `docs/verification.md`, and the prompts in `docs/prompts/`.
8. Ask the developer to approve the GitHub part (see Constraints), and wait.
9. Commit and push. Follow the push run until it passes.
10. Create the tag `v1.1.0` on the "Delivery" commit and a GitHub release (not a pre-release), with release notes. Follow the release run until it ends.
11. Check the package: the tags `1.1.0`, `sha-<commit>` and `latest` point to one digest, the one in the run's summary. Pull `1.1.0` and run `scripts/image-smoke-test.sh` against it with Docker Compose. Then restore the local image.
12. Record the release run and the package check in `docs/verification.md`, then commit and push that record (covered by the approval in step 8; it isn't part of the tag).
13. Report following AGENTS.md §9.

## Validation

1. **Commands:** every command added to the README and AGENTS.md was run as written, and its result matches what the docs say.
2. **Consistency:**
   - the docs don't contradict each other or the code: ports, profile, variables, URLs and job names;
   - `README.md` points to `v1.1.0` and to the "Delivery" section.
3. **Deliverables:** every Lab 05 deliverable listed in Task step 7 is present.
4. **CI:** the push run passes, with `publish` skipped.
5. **Release:**
   - the release run passes and `publish` runs;
   - `ghcr.io/vitorhsbarros/matriz` has `1.1.0`, `sha-<commit>` and `latest` on one digest;
   - the pulled `1.1.0` passes the smoke test.
6. **Tag:** `v1.1.0` points to the "Delivery" commit, on `main`.
7. **Working tree:** only the files in Constraints, plus this prompt, changed. The repository is in sync with `origin/main` at the end.

If a check fails, fix it and try again, at most twice. Then stop and ask.
