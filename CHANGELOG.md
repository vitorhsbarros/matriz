# Changelog

Every release of Matriz, newest first. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the versions follow [Semantic Versioning](https://semver.org/spec/v2.0.0.html). Each version is a Git tag on `main`.

## [1.2.0] - 2026-11-16

Ready to deploy on Render (PD13), plus the fixes from the final validation of the delivery ([docs/final-validation.md](docs/final-validation.md)). Release notes: [docs/release-notes/v1.2.0.md](docs/release-notes/v1.2.0.md).

### Added

- `MATRIZ_DB_URL` also accepts a PostgreSQL connection string (`postgresql://user:password@host:port/database`), the form hosting platforms give, besides a JDBC URL. It becomes a JDBC URL without its credentials; the user and password in it are used only when `MATRIZ_DB_USER` and `MATRIZ_DB_PASSWORD` aren't set. An invalid string stops the start-up without showing its value.
- `render.yaml`, a Render Blueprint: the application from the published image and a free PostgreSQL, in Frankfurt, with the health check on readiness and no secret in the file.
- `scripts/image-smoke-test.sh` can check a deployed application: with `BASE_URL`, it requires HTTPS and a `Secure`, `HttpOnly` session cookie, and skips the container checks.

### Changed

- The image built from the `Dockerfile` carries Matriz's own labels (`title`, `description`, `source`, and a version that is `local` unless the build sets it), instead of the base image's.
- The browser application's `package.json` has the release's version.

### Fixed

- `docs/architecture.md` named JUnit 5, while the build uses JUnit 6. `docs/delivery-plan.md` now gives the current name of the JAR next to the old one.

## [1.1.1] - 2026-11-16

Solves open points of 1.1.0. The application's features and its API are the same. Release notes: [docs/release-notes/v1.1.1.md](docs/release-notes/v1.1.1.md).

### Added

- A signed provenance attestation for each published image (GitHub artifact attestations), checked with `gh attestation verify oci://ghcr.io/vitorhsbarros/matriz:<version> -R vitorhsbarros/matriz`.
- `docs/deployment-options.md`, the deployment platforms compared for the pending decision PD13.

### Changed

- The version in `pom.xml` follows the releases (`1.1.1`, was `0.1.0-SNAPSHOT`), so the logs' `service.version` shows it. The JAR is `target/matriz.jar` whatever the version.
- Data access uses Spring JDBC only: Spring Data JPA and Hibernate, which were never used, were removed. The JAR is about half the size (31.8 MB instead of 60.6 MB) and starts faster.

### Fixed

- While the database can't be reached, `/actuator/health/readiness` answered `DOWN` only after 30 s. It now answers in about 5 s, the new time-out for getting a database connection.

## [1.1.0] - 2026-11-16

Container delivery (Lab 05). The application's features are the same as in 1.0.1. Release notes: [docs/release-notes/v1.1.0.md](docs/release-notes/v1.1.0.md).

Release candidate: [1.1.0-rc.1] (2026-11-15), a pre-release that checked the publication of the image, then promoted to 1.1.0.

### Added

- A container image: a multi-stage `Dockerfile` that builds the JAR and runs it on a Java 21 runtime as a non-root user, with an allow-list `.dockerignore`.
- The `app` service in `compose.yaml`, in the profile `app`: `docker compose --profile app up -d --wait` runs the application with PostgreSQL. `MATRIZ_APP_PORT` sets its host port.
- A CI workflow on GitHub Actions, on every push and pull request to `main`: the build and every test, SAST (Semgrep, CodeQL) and SCA (OSV-Scanner over a CycloneDX SBOM).
- A CI job, `image`, that runs after the tests, SAST and SCA: it builds the image, scans it with OSV-Scanner (failing on vulnerabilities that have a fix) and tests it with Docker Compose and `scripts/image-smoke-test.sh`.
- A CI job, `publish`, that pushes the tested image to `ghcr.io/vitorhsbarros/matriz` when a GitHub release is published, tagged with the version, the commit and, for a final release, `latest`.
- Public health checks `/actuator/health/liveness` and `/actuator/health/readiness`, with the database part of readiness only.
- JSON logs in the Elastic Common Schema in the container. Outside it, the logs stay plain text.
- `HealthChecksTest`, for the public health checks and the closed management endpoints.
- Documentation: "Technologies" and "Running with Docker" in the README, the delivery diagram in `docs/architecture.md`, `docs/delivery-plan.md`, this changelog and the release notes.

### Changed

- PostgreSQL is published by `compose.yaml` on `127.0.0.1` only, instead of every interface.
- `docker compose up -d --wait` still starts only PostgreSQL, but on the network `matriz` and with `restart: unless-stopped`.

### Security

- The image installs the Ubuntu security updates in its final stage. At the time this fixed USN-9065-1 (glibc), published after the base image was built.
- The application container runs with no Linux capabilities, `no-new-privileges` and a read-only file system.

## [1.0.1] - 2026-11-14

Tests and fixes from Lab 04. No new features.

### Added

- A test plan derived from the requirements (`docs/test-plan.md`).
- Unit tests (`TaskTitleRuleTest`, `MissingReferenceTest`), integration tests (`TaskApiPersistenceTest`, `DemoAccountsAuthorizationTest`, `ReviewRegressionTest`) and an end-to-end test in Chromium (`frontend/e2e/create-task.e2e.ts`), all run by `./mvnw verify`.

### Changed

- The API contract: the `type` of an error response is optional, and omitted when it is `about:blank` (RFC 9457).

### Fixed

- A NUL character in a text field gave `500`. It now gives `400` "The request body is not valid." (F1).
- Unrelated database errors were reported as "Project not found." or "Task not found.". Only a missing reference is now (F2).
- A NUL character in the dashboard's search gave `500`. It now matches no project (F4).

### Security

- Node updated from v24.15.0 to v24.22.3 for the build (F5), and `frontend-maven-plugin` from 1.15.4 to 2.0.2 (F6).

## [1.0.0] - 2026-11-12

The MVP (Lab 03): the 23 MVP user stories, usable through the browser application.

### Added

- Log-in and log-out with the demo accounts, server-side sessions with a CSRF token.
- The home dashboard with task counts and project search.
- Projects, their members, teams, tasks on a board with filters, assignees and comments.
- PostgreSQL with Flyway migrations, one folder per module, and module boundaries checked by `ModularityTest`.

[1.2.0]: https://github.com/vitorhsbarros/matriz/releases/tag/v1.2.0
[1.1.1]: https://github.com/vitorhsbarros/matriz/releases/tag/v1.1.1
[1.1.0]: https://github.com/vitorhsbarros/matriz/releases/tag/v1.1.0
[1.1.0-rc.1]: https://github.com/vitorhsbarros/matriz/releases/tag/v1.1.0-rc.1
[1.0.1]: https://github.com/vitorhsbarros/matriz/releases/tag/v1.0.1
[1.0.0]: https://github.com/vitorhsbarros/matriz/releases/tag/v1.0.0
