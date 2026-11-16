# Matriz — Final validation of the delivery

**Date:** 2026-11-16. **Version checked:** `v1.1.1`, `main` at `2fc566c`.
**How:** from a new clone of the GitHub repository in a temporary folder, following the README literally, with the `.env` made from `.env.example`. Port 8080 is taken on the reviewer's machine, so the application used `SERVER_PORT=8083` and `MATRIZ_APP_PORT=8082`, the overrides the README describes.

**Verdict: ready for delivery, with reservations:** the limitations below. No defect was found in the application, its tests, its image or its pipeline. The four small problems found were fixed after the check, with the developer's approval.

## Requirements met

| # | Check | Result | Evidence |
|---|---|---|---|
| 1 | Run from the README | **Met.** Every step worked as written: `cp .env.example .env` (its fictitious values work as they are), `docker compose up -d --wait`, `./mvnw verify`, `java -jar target/matriz.jar`, the three health URLs `UP`, `/` served, the three demo accounts log in. Then "Running with Docker": `build`, `up -d --wait`, the health URLs, the logs, `scripts/image-smoke-test.sh`, `down` | `./mvnw verify`: BUILD SUCCESS in 2 min 11 s, 221 back-end tests, 46 browser application tests, the end-to-end test (Chromium) "1 passed". The JAR started in 1.9 s |
| 2 | Containers | **Met.** `app` and `postgres` both `healthy`, ports on `127.0.0.1` only (`8082->8080`, `5432->5432`). `app` runs as user 10001, with a read-only file system, no capabilities and `no-new-privileges`; both with `restart: unless-stopped`. After `down`, the volume stayed | `docker compose ps`, `docker inspect` |
| 3 | Essential features | **Met, 14 of 14, against the JAR and again against the container.** Log-in, who is logged in, the dashboard with counts, create a task (priority High), move it to In progress, edit it, filter the board, comment, delete it, log-out (then 401). Access rules: Rui can't edit *Website* (403 "Only the project manager can edit the project."), but sees its tasks; Sara gets 404 "Project not found.". The smoke test's 12 checks passed, including persistence after recreating the containers | The `curl` checks, `scripts/image-smoke-test.sh` |
| 4 | CI/CD | **Met.** The last 10 runs of `ci.yml` passed, among them the release runs of `v1.1.0` and `v1.1.1`. In 25 runs, the only 2 failures are the two deliberate gate checks (Lab 04 and Lab 05, exercise 4). No annotation in the last two runs. The package is public, with `1.1.1` and `latest` on one digest. `gh attestation verify` on `1.1.1`: valid, built by `ci.yml` at `v1.1.1` from `ef0975e` | Runs [43789322007](https://github.com/vitorhsbarros/matriz/actions/runs/43789322007) (`main`) and [43788641658](https://github.com/vitorhsbarros/matriz/actions/runs/43788641658) (release `v1.1.1`) |
| 5 | Logs | **Met.** JAR: 29 log lines, all INFO. Container: 21 lines, all ECS JSON, all INFO. PostgreSQL: no error. None has an email address or a password. Build: one npm warning, about `fsevents`'s install script, macOS only (already known since Lab 04); the CI build log has none | The logs of this check |
| 6 | No credentials in versioned files | **Met.** Semgrep `p/secrets` (1.183.0, the CI's rules): 0 findings in 184 files. Pattern searches for private keys, tokens, cloud keys and connection strings with a password: 0, in the files and in the 60 commits of the history. The password-like assignments found are all fictitious: `change-me-…` examples, a password generated for each end-to-end run, test literals, and example values in `openapi.yaml` (one of them an example CSRF token in Lab 02's version). No `.env` was ever committed | The scans of this check |
| 7 | Final review | **Met, after fixing the four details below.** The README, AGENTS.md, CHANGELOG, release notes, Dockerfile, `compose.yaml` and workflow agree with each other and with what was observed: commands, ports, profile, variables, health URLs, job names, versions | This check |

## Problems found

| # | Problem | Kind | Fix (applied) |
|---|---|---|---|
| P1 | `docs/architecture.md` §5 says "JUnit 5", while the build uses JUnit 6.0.3 (managed by Spring Boot 4.1.2); the README says 6.0.3 | Documentation | Now says "JUnit 6 (Jupiter, managed by Spring Boot)" |
| P2 | `docs/delivery-plan.md` §1 and §2 name `matriz-0.1.0-SNAPSHOT.jar`, from the analysis of 2026-11-15, with no note that the JAR is `target/matriz.jar` since `1.1.1` | Documentation | Both lines now give the current name too |
| P3 | `frontend/package.json` says version `0.1.0`. The package is private and its version isn't used, but it doesn't match the release | Consistency | `1.1.1` in `package.json` and `package-lock.json`. `npm ci` accepts the lock |
| P4 | An image built locally carries the base image's `org.opencontainers.image.version` label (`24.04`): only CI sets Matriz's labels. The published images are right (`v1.1.1`) | Image metadata | The `Dockerfile` sets `title`, `description` and `source`, and the version from a build argument, `local` by default. A local build now shows `version=local`; labels given at build time, as CI does, still win (checked with `--label`) |

None of them changes how Matriz runs. After the fixes, `./mvnw verify` passed again (221, 46 and 1 tests), and so did `scripts/image-smoke-test.sh` on the rebuilt image. They come after `v1.1.1`, so they are listed under "Unreleased" in the [CHANGELOG](../CHANGELOG.md), and the published `1.1.1` image keeps its labels.

## Limitations still open

- **Not deployed:** the deployment platform (PD13) is the developer's decision. The options are in [deployment-options.md](deployment-options.md).
- **amd64 only:** the published image is built for `linux/amd64`, and runs under emulation on arm64.
- **The Java runtime isn't scanned:** the image scan covers the Ubuntu packages, the dependency scan the Java and npm libraries.
- **Browsers:** the end-to-end test runs in Chromium. Firefox and Safari weren't run in this check, and WebKit was run once by hand in Lab 04.
- **The published image** was checked through its CI run and its attestation. It wasn't pulled and run again in this check, as it was in [verification.md §20](verification.md#20-release-v111-the-open-points-of-v110).
