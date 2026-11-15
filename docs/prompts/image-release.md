## Role

You are a senior DevOps engineer responsible for releases. You treat every published image as part of the software supply chain: only code that passed every check is published, write permissions are minimal, and each image can be traced back to its source.

## Goal

Publish the Matriz image to the GitHub Container Registry (`ghcr.io`) automatically when a GitHub release is published, following step 4 of `docs/delivery-plan.md` (Challenge C):
- the image published is exactly the one the `image` job built, scanned and tested for that release's commit;
- only a published release triggers publication. Pushes, pull requests and manual runs never publish;
- the job that publishes is the only one with `packages: write`, and it logs in with the workflow's `GITHUB_TOKEN`;
- it is shown working with a test pre-release, `v1.1.0-rc.1`.

## Context

- **Read first:** `.github/workflows/ci.yml`, `docs/delivery-plan.md` (§5.4 and §7), the `Dockerfile`, `compose.yaml`, `scripts/image-smoke-test.sh`, `docs/verification.md` (§13 and §16) and `AGENTS.md`.
- **The pipeline today:** five jobs: `build`, `sast-semgrep`, `sast-codeql`, `sca` and `image`. `image` runs after the other four. It builds the image with Buildx (`load: true`), scans it with OSV-Scanner, and tests it with Docker Compose and `scripts/image-smoke-test.sh`. Nothing is published. Every action is pinned to a commit SHA, with the version in a comment.
- **The image:** built from the `Dockerfile` for `linux/amd64` on the runner. Its final stage installs Ubuntu security updates, so two builds of the same commit may differ.
- **Releases:** the last release tag is `v1.0.1` (Lab 04). The repository `vitorhsbarros/matriz` is public. No package has been published to `ghcr.io` yet.
- **Approved in the delivery plan (2026-11-15):** the GitHub Container Registry, publication only from releases (version tags), tags by version and commit, `packages: write` only in the publishing job, and the `GITHUB_TOKEN`.
- **Pending decision:** the deployment platform (PD13). Publishing an image doesn't deploy it anywhere.

## Constraints

- **Security:**
  - only the `publish` job has `packages: write`, together with `contents: read`; no other job's permissions change;
  - log in to `ghcr.io` with `GITHUB_TOKEN` through `docker login --password-stdin`; no stored secrets, and no `docker/login-action`;
  - the only new action is `actions/download-artifact`, pinned to the commit SHA of a release at least two weeks old, with the version in a comment;
  - the image is handed from `image` to `publish` as a short-lived artifact (1 day). It holds no secrets: the throw-away `.env` is never part of it.
- **Tags:**
  - `ghcr.io/vitorhsbarros/matriz:<version>` (the release tag without the leading `v`) and `:sha-<short commit>`;
  - `:latest` only for a release that is not a pre-release;
  - the image carries OCI labels for its source repository, revision and version.
- **Trigger:**
  - add `release: types: [published]` to the workflow;
  - `publish` runs only when `github.event_name == 'release'`, and only after `image` has passed.
- **Scope:**
  - don't change the `Dockerfile`, `compose.yaml`, the smoke test or the application code without asking;
  - multi-platform images are out of scope (amd64 only);
  - don't make PD13;
  - if the package is created as private, report it and ask before changing its visibility.
- **Approval:** before the GitHub part starts, ask the developer once to approve:
  - the commit and push to `main`;
  - creating the tag and the pre-release `v1.1.0-rc.1`, which publishes the first package to `ghcr.io`;
  - the commit and push of the record at the end.

  Nothing is pushed or published before that approval.
- **Commits:** messages start with "Lab 05, challenge C:". The workflow in one commit, the record in `docs/verification.md` in another.
- **Language:** files and commit messages in English, the conversation in the developer's language.

## Task

1. Read the sources in Context. Choose the release of `actions/download-artifact` (at least two weeks old) and look up its commit SHA.
2. In `ci.yml`:
   - add the `release` trigger;
   - in `image`, add the OCI labels to the build, and on a release only, save the tested image (`docker save`) and upload it as an artifact with `actions/upload-artifact` (1 day);
   - add the `publish` job: it needs `image`, runs on releases only, and has `contents: read` and `packages: write`. It downloads the artifact, loads the image, works out the tags, logs in with `GITHUB_TOKEN`, pushes the tags, and writes the pushed tags and digest to the run's summary.
3. Validate the workflow with `actionlint`.
4. Ask the developer to approve the GitHub part (see Constraints), and wait.
5. Commit and push to `main`. Follow the push run, and confirm that `publish` is skipped there.
6. Create the tag and the pre-release `v1.1.0-rc.1` on that commit, with short release notes. Follow the release run until it ends.
7. Check the published package:
   - its visibility and the tags on `ghcr.io`;
   - pull it by version on the developer's machine (it runs under emulation on arm64);
   - check that its digest matches the run's summary and its labels point to the repository and commit;
   - run it with `docker compose --profile app` and `scripts/image-smoke-test.sh`.
8. Update `docs/delivery-plan.md` §7 to mark steps 1 to 4 as done, and record everything in `docs/verification.md`, with links to the runs, the release and the package.
9. Report following AGENTS.md §9.

## Validation

1. **Workflow:** `actionlint` passes. `publish` is the only job with `packages: write`.
2. **No publication without a release:** on the push run, every job passes and `publish` is skipped.
3. **Release run:** every job passes, `publish` runs after `image`, and the log shows no token or secret.
4. **Package:**
   - `ghcr.io/vitorhsbarros/matriz` has the tags `1.1.0-rc.1` and `sha-<commit>`, and no `latest`;
   - the package is linked to the repository;
   - its visibility is reported.
5. **Same image:** the pulled image's digest matches the one in the run's summary, and its labels show the release's commit.
6. **It runs:** the pulled image passes `scripts/image-smoke-test.sh` with Docker Compose.
7. **Working tree:** only `ci.yml`, `docs/delivery-plan.md`, `docs/verification.md` and the prompt changed.

If a check fails, fix it and try again, at most twice. Then stop and ask.
