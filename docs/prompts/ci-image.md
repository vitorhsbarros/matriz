## Role

You are a senior DevOps engineer. You review every job and permission from a security point of view: least privilege, pinned actions, and no secrets in logs.

## Goal

Extend the existing CI pipeline (`.github/workflows/ci.yml`) to automate the quality checks and the build of Matriz, following step 3 of `docs/delivery-plan.md`:
- it runs automatically on `push` and `pull_request`;
- it installs the dependencies, runs the existing automated tests and validates the build of the application;
- it builds the Docker image, but only after the required checks have passed, then scans and tests that image;
- it uses minimal permissions and exposes no secrets or credentials;
- the results are checked in the Actions section of GitHub.

Required results:
- the pipeline fails when a required test fails;
- this is shown with a temporary, controlled failure in a test, which is then fixed and the checks run again;
- there is evidence on GitHub of an approved run.

## Context

- **Read first:** `.github/workflows/ci.yml`, `docs/delivery-plan.md` (§5.4 and §8), the `Dockerfile`, `compose.yaml`, `.env.example`, `docs/verification.md` (§13 to §15) and `AGENTS.md`.
- **The current pipeline** (Lab 04) runs on `push` and `pull_request` to `main` and on `workflow_dispatch`, with `permissions: contents: read` at the top. It has four jobs:
  - `build`: `./mvnw verify` on `ubuntu-24.04`, which runs the 213 back-end tests, the 46 browser application tests, `ModularityTest` and the end-to-end test. It keeps the test reports when it fails;
  - `sast-semgrep` and `sast-codeql`: the code analysis. Only `sast-codeql` has `security-events: write`;
  - `sca`: OSV-Scanner over a CycloneDX SBOM and the npm lockfile.

  Every action is pinned to a commit SHA, with the version in a comment.
- **The image:** the `Dockerfile` (exercise 2) builds the JAR without tests and produces a non-root image with a health check. `compose.yaml` (exercise 3) runs it with PostgreSQL through the `app` profile, and reads its values from `.env`.
- **What no check covers yet:** the operating system packages of the image (Ubuntu 24.04) and the Java runtime. The SCA job only reads the Maven and npm dependencies (`docs/verification.md` §14, "Not checked").
- **Smoke test:** following `docs/delivery-plan.md` §5.4, the new job starts the image with `docker compose --profile app up --wait` and tests it, as was done by hand in `docs/verification.md` §14 and §15. The runner has no `.env`, so the job writes one with values generated in the job itself (random passwords), masked so they never appear in the logs.
- **Platform:** CI runs on amd64, while the image was so far only built on the developer's arm64 Mac.
- **The repository is public:** its workflow runs and logs can be seen by anyone.
- **Earlier failure proof:** in Lab 04 a throw-away branch, `ci-gate-check`, with one broken assertion in `TaskTitleRuleTest`, was run with `workflow_dispatch`. Then the branch was deleted. The evidence is in `docs/verification.md` §13.

## Constraints

- **Security:**
  - minimal permissions: the new job gets `contents: read` and nothing else; no job gains a write permission;
  - new actions: only the approved `docker/setup-buildx-action` and `docker/build-push-action` (and no `docker/login-action` until publication). They are pinned to the commit SHA of a release at least two weeks old, with the version in a comment, as in the existing workflow;
  - no stored GitHub secrets;
  - the values generated for the smoke test are masked with `::add-mask::` and never printed. The runner's `.env` is deleted at the end of the job, even when it fails.
- **Image scan:** OSV-Scanner only, already approved and pinned; no new scanning tool. It fails only on vulnerabilities that have a fix, and lists the others. If OSV-Scanner can't tell them apart, report it and ask.
- **Order:** the image job needs every existing check: `build`, `sast-semgrep`, `sast-codeql` and `sca`. If any of them fails, the image isn't built.
- **Scope:**
  - publication to `ghcr.io` (step 4 of the plan) is out of scope; the image isn't pushed anywhere;
  - don't make the pending decision PD13;
  - don't change the `Dockerfile`, `compose.yaml` or the application code; the only new file besides the workflow is `scripts/image-smoke-test.sh`. If CI can't work without another change, stop and ask.
- **Approval:** before the GitHub part starts, ask the developer once to approve all of it:
  - the commit and push to `main`;
  - a throw-away branch with the controlled failure;
  - opening and closing a pull request from that branch, without merging;
  - deleting the branch.

  Nothing is pushed before that approval.
- **Commits:** messages start with "Lab 05, exercise 4:". The workflow and the script in one commit, the record in `docs/verification.md` in another.
- **Language:** files and commit messages in English, the conversation in the developer's language.

## Task

1. Read the sources in Context. Choose the releases of `docker/setup-buildx-action` and `docker/build-push-action` (at least two weeks old) and look up their commit SHAs.
2. Write `scripts/image-smoke-test.sh`, which checks a running Compose setup through its URL. It never prints secrets:
   - **image:**
     - the container runs as a non-root user;
     - the image has no `.env`, source code or Node;
     - the root file system is read-only;
     - the published ports are bound to `127.0.0.1`;
   - **application:**
     - `/actuator/health` answers `UP`;
     - `/` and its script are served;
     - a demo account logs in;
     - creating a task without the CSRF header gives 403;
     - with the header, the task is created (201) and read back;
   - **persistence:** after `docker compose --profile app down` (without `-v`) and `up`, the task is still there. Then it is deleted;
   - **result:** the script exits non-zero on the first failed check, naming it.
3. Add an `image` job to `ci.yml`:
   - it needs `build`, `sast-semgrep`, `sast-codeql` and `sca`;
   - checkout, then Buildx;
   - build the image with the `type=gha` cache and `load: true`;
   - scan it with OSV-Scanner (the version already pinned in the workflow). It fails only on vulnerabilities that have a fix, and lists the others in the job's output;
   - write a `.env` with generated, masked values;
   - `docker compose --profile app up -d --wait`, then run the script;
   - on failure, print the Compose logs;
   - always run `docker compose --profile app down -v` and delete the `.env`.
4. Run the script locally against the developer's Compose setup, and validate the workflow's YAML with a parser. Use `actionlint` only if it runs without installing anything.
5. Ask the developer to approve the GitHub part (see Constraints), and wait.
6. Commit and push the workflow and the script to `main`, and follow the run until it ends.
7. **Controlled failure,** through a pull request:
   - on a throw-away branch, break one assertion in `TaskTitleRuleTest` and open a pull request to `main`;
   - confirm that `build` fails and that `image` doesn't run;
   - fix the assertion on the same branch, and confirm that every job passes, `image` included;
   - close the pull request without merging it, and delete the branch.
8. Record everything in `docs/verification.md`, with links to the runs, and commit it after approval.
9. Report following AGENTS.md §9.

## Validation

1. **Locally:**
   - the script passes against the developer's Compose setup;
   - it fails, naming the check, when one check fails (e.g. a wrong port);
   - the workflow's YAML is valid.
2. **On GitHub, the run on `main`:**
   - all five jobs pass;
   - `image` starts only after the other four;
   - the job shows the image scan's result;
   - the logs show no secret values.
3. **Image scan:** it passes, or fails only on vulnerabilities that have a fix. The others are listed.
4. **Controlled failure:**
   - with the broken test, `build` fails and `image` is skipped;
   - after the fix, every job passes;
   - the pull request is closed without merging, and the branch is deleted.
5. **Permissions:** no job gained a write permission, and `image` has only `contents: read`.
6. **Working tree:** only `ci.yml`, `scripts/image-smoke-test.sh`, `docs/verification.md` and the prompt changed.

If a check fails, fix it and try again, at most twice. Then stop and ask.
