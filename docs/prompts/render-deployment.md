## Role

You are a senior DevOps engineer who deploys containerised applications to PaaS platforms. You keep every secret out of the repository, give each service only what it needs, describe the infrastructure as code, and check the result from the outside, over HTTPS, as a user would.

## Goal

Put Matriz online on Render, over HTTPS, for the final presentation in 2 days (PD13, decided 2026-11-16: Render with Render's PostgreSQL):
- Render runs the published, tested and attested image from `ghcr.io`, with Render's free PostgreSQL;
- the infrastructure is described in a `render.yaml` Blueprint in the repository, so the developer's part in Render is creating the Blueprint and entering the secrets;
- the public address answers over HTTPS, the demo accounts work, and the presentation has a short checklist.

## Context

- **Read first:** `docs/deployment-options.md` (the decision and Render's free plan), `docs/constraints.md` (PD13, SEC and PRI rules), `docs/architecture.md` §5, `src/main/resources/application.yaml`, `.env.example`, the `Dockerfile`, `compose.yaml`, `scripts/image-smoke-test.sh`, `.github/workflows/ci.yml`, `CHANGELOG.md`, `README.md` and `AGENTS.md`.
- **The application:**
  - Spring Boot, one container; health checks at `/actuator/health/liveness` and `/actuator/health/readiness`; ECS JSON logs;
  - it listens on `SERVER_PORT` (8080);
  - every setting is an environment variable. The database is set with `MATRIZ_DB_URL` (JDBC), `MATRIZ_DB_USER` and `MATRIZ_DB_PASSWORD`;
  - the session cookie is `Secure` by default (`MATRIZ_COOKIE_SECURE`);
  - `MATRIZ_DEMO_SEED=true` creates the three demo accounts and two projects on an empty database, and needs `MATRIZ_DEMO_PASSWORD_ANA`, `_RUI` and `_SARA` (8 to 128 characters).
- **The image:** `ghcr.io/vitorhsbarros/matriz:1.1.1` (public, `linux/amd64`, with a provenance attestation), published by CI when a GitHub release is published. `CHANGELOG.md` has "Unreleased" fixes from the final validation (image labels, package version, two document fixes).
- **Render's free plan** (its documentation, checked 2026-11-16):
  - a web service sleeps after 15 minutes without traffic and takes about a minute to wake up;
  - the free PostgreSQL expires 30 days after it is created, has 1 GB and no backups;
  - Render gives its database's host, port, name, user and password separately, and a `postgresql://` connection string, but no JDBC URL.
- **Who does what:** the assistant has no access to the developer's Render account. Creating the account, connecting GitHub, creating the Blueprint and entering the secrets are the developer's steps.
- **The developer's machine:** port 8080 is taken; local checks use `MATRIZ_APP_PORT=8082`.

## Constraints

- **Secrets:**
  - no secret in `render.yaml` or anywhere in the repository;
  - the demo passwords are chosen by the developer and entered in Render's dashboard (`sync: false`);
  - the database credentials come from the database itself (`fromDatabase`);
  - never print a secret in the conversation, the logs or the records.
- **Application change, the only one:** in `application.yaml`, when `MATRIZ_DB_URL` isn't set, build it from `MATRIZ_DB_HOST`, `MATRIZ_DB_PORT` (default 5432) and `MATRIZ_DB_NAME`. Every existing setting keeps working as before. Covered by a test.
- **Release:** that change reaches Render only through a new image, so release `v1.2.0` (minor: a new, backward-compatible way to configure the database):
  - it includes the "Unreleased" fixes;
  - set `pom.xml` and `frontend/package.json` (and its lock) to `1.2.0`;
  - CHANGELOG and release notes.
- **Render's set-up:**
  - `render.yaml` describes a free PostgreSQL (the newest major version Render offers, at most 17) and a free web service running `ghcr.io/vitorhsbarros/matriz:1.2.0`, pinned by version;
  - region Frankfurt;
  - health check on `/actuator/health/readiness`;
  - the service gets its port from Render's `PORT`;
  - `JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=75`;
  - `MATRIZ_COOKIE_SECURE` stays `true`;
  - the database accepts connections from Render's private network only, if the free plan allows it.
- **Check before writing:** confirm every `render.yaml` key and every free-plan rule in Render's current documentation (Blueprint specification, free instances, image-backed services, ports, health checks). Anything not confirmed is reported, not guessed.
- **Out of scope:** automatic deployment from CI (CD), a custom domain, paid plans, and Neon.
- **Approval:** ask the developer once, before anything leaves the machine, to approve:
  - the commits and pushes to `main`;
  - the tag and the GitHub release `v1.2.0`, which publishes `1.2.0` and `latest` to `ghcr.io`.
- **Commits:** messages start with "Lab 05:". Files and commit messages in English, the conversation in the developer's language.

## Task

1. Read the sources in Context, and check Render's current documentation (see Constraints). Note every fact used, with its link.
2. **The database URL:**
   - change `application.yaml` as in Constraints;
   - add a test showing that the URL is built from the three variables, and that `MATRIZ_DB_URL` still wins when set;
   - run `./mvnw verify`.
3. **The smoke test against a public address:** give `scripts/image-smoke-test.sh` a mode for a remote base URL (e.g. `BASE_URL=https://…`). In that mode it:
   - skips the container and persistence checks;
   - runs the health, browser application, log-in, CSRF and task checks;
   - also checks that the session cookie is `Secure`;
   - takes the demo password from an environment variable.
4. **`render.yaml`:** the database and the web service, as in Constraints, with comments. Validate it against the Blueprint specification.
5. **Release `v1.2.0`:** versions, CHANGELOG ("Unreleased" becomes `1.2.0`), and `docs/release-notes/v1.2.0.md`.
6. **Documentation:**
   - add "Deploying to Render" to the README, with the developer's steps in order:
     1. create a Render account, and connect GitHub;
     2. "New → Blueprint", and choose the repository;
     3. enter the three demo passwords;
     4. "Apply";
     5. wait for the first deploy;
     6. open the `onrender.com` address;
   - mention the free plan's limits;
   - record the deployment in `docs/deployment-options.md`.
7. Run the local checks:
   - `./mvnw verify`;
   - the image built and run with Compose, with the new variables (`MATRIZ_DB_HOST`, `_PORT`, `_NAME` instead of `MATRIZ_DB_URL`);
   - the smoke test, in both modes;
   - `actionlint`.
8. Ask the developer for the approval in Constraints, and wait. Then commit, push, follow the CI run, create the release `v1.2.0`, and follow the release run until `1.2.0` is published and attested.
9. **Hand over to the developer:** give them the Render steps, and wait while they create the Blueprint. Ask them for the public address only, never the passwords.
10. **Check the deployment from the outside:**
    - HTTPS and its certificate;
    - the three health URLs;
    - `/` served;
    - the remote smoke test, with the developer running it or providing Ana's password through an environment variable that is never printed;
    - the time of a first request after a sleep, if it can be observed.
11. Record the deployment in `docs/verification.md` (§21), with the address, the checks and their results. Give the developer a checklist for the presentation day. Commit and push the record.
12. Report following AGENTS.md §9.

## Validation

1. **Tests:** `./mvnw verify` passes, the new test included. The image runs with either way of setting the database.
2. **Secrets:** `render.yaml`, the docs and the records contain no secret. Semgrep `p/secrets` (as in CI) passes on the changed files.
3. **Blueprint:** every key in `render.yaml` is in Render's documentation, with the link recorded.
4. **Release:** the CI and release runs pass; `1.2.0` and `latest` are on one digest, with a valid attestation (`gh attestation verify`).
5. **Online:**
   - `https://<service>.onrender.com/actuator/health/readiness` answers `UP` over a valid certificate;
   - the remote smoke test passes;
   - the session cookie is `Secure`;
   - Sara gets "Project not found." for *Website*.
6. **Handover:** the README's Render steps were the ones the developer actually followed, corrected where they differed. The presentation checklist is given.

If a check fails, fix it and try again, at most twice. Then stop and ask.
