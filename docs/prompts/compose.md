## Role

You are a senior DevOps engineer. You review every service and setting from a security point of view: least privilege, no secrets, and minimal exposure.

## Goal

Make Matriz runnable through Docker Compose, following step 2 of `docs/delivery-plan.md`:
- update `compose.yaml` with the services of the architecture: the application (the image from the `Dockerfile`) and PostgreSQL;
- configure the network and the communication between the services;
- keep a persistent volume for the database data;
- update `.env.example` with every configuration variable the Compose setup needs;
- give each service a suitable health check, and start the application only when the database is healthy;
- run and validate the whole application.

Required results:
- the application is reachable;
- the components communicate;
- the main features still work;
- the database data survives recreating the containers;
- the logs show no unresolved critical errors.

## Context

- **Read first:** `docs/delivery-plan.md` (§3, §5.1 and §5.3), `AGENTS.md`, the current `compose.yaml` and `.env.example`, the `Dockerfile`, `src/main/resources/application.yaml`, and `docs/verification.md` §14.
- **What exists:**
  - `compose.yaml` has one service, `postgres` (PostgreSQL 17.10), with the named volume `matriz-postgres` and a health check (`pg_isready`). It reads `MATRIZ_DB_NAME`, `MATRIZ_DB_USER` and `MATRIZ_DB_PASSWORD` from `.env`.
  - The application image (`Dockerfile`) was built and validated in the previous exercise: it runs as a non-root user, listens on port 8080, has a health check on `/actuator/health`, and takes every setting from environment variables.
  - `README.md` and `AGENTS.md` §10 document `docker compose up -d --wait` as the command that starts the database for local development.
- **Configuration:**
  - `MATRIZ_DB_URL` in `.env` points to `localhost`, for the JAR run on the host. Inside Compose the application must reach the database by its service name, so the `app` service needs its own URL.
  - The local `.env` sets `MATRIZ_COOKIE_SECURE=false`, because the checks use `http://localhost`.
- **Containers:** two, as decided in `docs/delivery-plan.md` §5.1. The browser application is inside the application image.
- **Environment:**
  - port 8081 on the host is taken by a local `java -jar` of the application. Leave it running;
  - free disk space is low (about 3 GB): check it before building.

## Constraints

- **Security:**
  - no secrets in `compose.yaml`: every value comes from `.env`, which is never committed;
  - `.env.example` holds only fictitious values;
  - both published ports are bound to `127.0.0.1` only. This includes PostgreSQL, which today listens on every interface;
  - the services share a named network, `matriz`.
- **Ports:** the application is published on host port `MATRIZ_APP_PORT`, 8080 by default, and the variable is listed in `.env.example`. PostgreSQL keeps port 5432.
- **Compatibility:** the `app` service is in the Compose profile `app`, so `docker compose up -d --wait` still starts only the database, as `README.md` and `AGENTS.md` describe.
- **Scope:**
  - don't change the `Dockerfile`, the application code or the CI workflow;
  - don't make the pending decision PD13;
  - if something can't work without one of those changes, stop and ask.
- **Data:** never remove the `matriz-postgres` volume or its data. No `docker compose down -v`.
- **Secrets during the checks:** never print values from `.env` in the output or in the records.
- **Commits:** only after the developer approves. The configuration in one commit, the record in `docs/verification.md` in another. Messages start with "Lab 05, exercise 3:".
- **Language:** files and commit messages in English, the conversation in the developer's language.

## Task

1. Read the sources in Context and check the free disk space.
2. Update `compose.yaml`:
   - add the `app` service, in the `app` profile:
     - built from the `Dockerfile` (`build: .`) and tagged `matriz:local`;
     - it starts only when `postgres` is healthy (`depends_on` with `condition: service_healthy`);
     - it gets its settings as an explicit list in `environment:`, with values from `.env`, and its own `MATRIZ_DB_URL` pointing to the `postgres` service;
     - it is published on `127.0.0.1:${MATRIZ_APP_PORT:-8080}`;
   - bind the PostgreSQL port to `127.0.0.1`;
   - put both services on the named network `matriz`;
   - keep the `matriz-postgres` volume;
   - set `restart: unless-stopped` on both services.
3. Update `.env.example`: add `MATRIZ_APP_PORT` and a note about running everything with Compose.
4. Check the file with `docker compose config --quiet`, and with `docker compose --profile app config --services`. Never print the resolved values.
5. Start everything with `docker compose --profile app up -d --build --wait`. Then check `docker compose --profile app ps` and `docker compose --profile app logs`.
6. Run the checks in Validation.
7. Prove that the data persists:
   - create a task;
   - recreate the containers with `docker compose --profile app down`, without `-v`, and then `up` again;
   - read the task back, then delete it.
8. Leave things as they were: stop the `app` service, and keep only the database running.
9. Record the checks and their results in `docs/verification.md`.
10. Report following AGENTS.md §9, and ask for approval before the commits.

## Validation

1. **Valid file:** `docker compose config --quiet` passes. `config --services` lists `postgres` and `app` with the `app` profile, and only `postgres` without it.
2. **Compatibility:** without the profile, `docker compose up -d --wait` still starts only the database.
3. **Start-up:** both services are `healthy`. The logs show that `app` started after `postgres` was healthy, the Flyway migrations and "Started", with no unresolved ERROR.
4. **Reachable:** `/actuator/health` answers `UP`, and `/` serves the browser application.
5. **Communication:** `app` reaches `postgres` by its service name on the `matriz` network.
6. **Main features:** a demo account logs in. Creating a task without the CSRF header is refused (403). With the header, the task is created (201) and read back.
7. **Persistence:** the task is still there after `down` and `up` (without `-v`), and is deleted afterwards.
8. **Exposure:** `docker compose ps` shows both ports bound to `127.0.0.1` only. The `app` container has only the listed variables.
9. **No secrets:** `compose.yaml` has no secret values, and `.env.example` has only fictitious ones.
10. **Working tree:** `git status` shows only `compose.yaml`, `.env.example`, `docs/verification.md` and the prompt.

If a check fails, fix it and try again, at most twice. Then stop and ask.
