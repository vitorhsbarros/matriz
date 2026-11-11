## Role

You are a senior Java back-end developer experienced in Spring Security and session-based authentication. You write secure, tested code, and you think like an attacker when you check the access rules.

## Goal

Identify the user and enforce the access rules, in the back end first:
- Users can log in, view their profile and log out, using the authentication mechanism already chosen.
- The back end takes the user's identity from the authentication, never from values the client sends.
- A reusable authorisation mechanism checks every operation on a project and on its tasks, comments and assignments, including direct requests to the API. It is applied to the endpoints that exist now, and each later increment uses it.
- The browser application sends the user to the log-in page when needed, and shows an understandable message when the session is no longer valid.

## Context

- **Project:** Matriz, a simplified Jira-like project and task management application. It is a modular monolith in Java 21 and Spring Boot 4, with PostgreSQL and Flyway and a React + TypeScript browser application served from the same JAR. The modules are `matriz.{web,accounts,projects,teams,tasks}`. Types in the top-level package of a module are its public interface, and `internal` sub-packages are private. `ModularityTest` checks these boundaries.
- **Read first:**
  - `AGENTS.md`;
  - `docs/implementation-plan.md` (access matrix, increment I1, operations per increment);
  - US19 and US02 in `docs/user-stories.md`;
  - the SEC and PRI rules and PD02 in `docs/constraints.md`;
  - ADR-0002 (sessions), through `docs/architecture.md`;
  - `openapi.yaml` and `docs/api-contract.md`.
- **What already exists (increment I0, done):**
  - `web/internal/SecurityConfig` denies every request except `/`, the static files and `/actuator/health`.
  - The Spring Session JDBC tables exist. The cookie is `MATRIZ_SESSION`, with HttpOnly, Secure and SameSite=Lax.
  - Passwords are stored as BCrypt hashes through `DelegatingPasswordEncoder` (PD06).
  - `accounts.AccountDirectory` is the public interface of the Accounts module.
  - The seed creates three made-up demo accounts and two projects. In *Website*, Ana is the manager, Rui is a member and Sara is not a member. In *App*, Rui is the manager and Ana is a member.

## Constraints

- **Scope:**
  - increment I1: US19 and US02, with DoD07;
  - a reusable authorisation mechanism, applied to the endpoints that exist now;
  - the browser application's log-in redirect.

  The endpoints of tasks, comments and assignments stay in their increments. Don't add features, fields or endpoints that no story asks for (AGENTS.md §3).
- **Identity:** take the user's identity only from the authenticated session. The back end works out the project manager and the comment author itself. Never accept a user id from the client as proof of identity.
- **Mechanism:** use server-side sessions with Spring Session JDBC (ADR-0002) and CSRF protection. Don't use JWT, and don't store a token in `localStorage`.
- **Passwords:** store only hashes made with the existing `DelegatingPasswordEncoder` (BCrypt). Use only made-up accounts and data. Never write secrets into code, documents or logs.
- **No account enumeration:** a response must never reveal whether a user with a given email exists. Wrong email and wrong password give the same response and message: `401` "Invalid email or password.".
- **Status codes and messages,** exactly as in the acceptance criteria and `docs/api-contract.md`:
  - `401` "Please log in to continue." when the user isn't logged in or the session is no longer valid;
  - `404` "Project not found." for anyone who isn't a member of the project (DoD03), so the project's existence isn't revealed;
  - `403` only for a member who tries a manager-only action, and for an invalid CSRF token.
- **Pending decision PD02 (session expiry):** don't decide it. Propose options with their trade-offs and wait for the user's choice.
- **Approval:** present a short plan before changing code, and wait for approval (AGENTS.md §4). The plan lists the files, the reasons and the checks. A new dependency, such as `spring-security-test`, needs explicit approval (AGENTS.md §6).
- **Conventions:** respect the module boundaries checked by `ModularityTest`. Write code and documents in English.

## Task

1. **Prepare.** Read the documents listed in Context. Then present, and wait for approval:
   - the options for PD02 (session expiry), with their trade-offs;
   - a short plan with the files to change, the reasons, the new dependencies and the checks.
2. **Log-in, profile and log-out.** Implement them with the operations and responses that `openapi.yaml` defines for US19 and US02:
   - log-in creates a session, and renews the session id;
   - the profile returns the logged-in user;
   - log-out invalidates the session.
3. **Authorisation.** Implement a reusable check that answers two questions for the logged-in user: are they a member of the project, and are they its manager? It returns the responses in Constraints. Apply it to every endpoint that exists now. The later increments will use it for projects, tasks, comments and assignments.
   - **Testing it:** test it through a test-only endpoint in `src/test`. If a real project endpoint is needed earlier than the plan says, propose it in the plan instead of adding it.
4. **Browser application:**
   - a log-in screen;
   - the user's name and a log-out action;
   - on any `401` response, send the user to the log-in screen and show "Please log in to continue.";
   - send the CSRF token with every request that changes data.
5. **Documentation:**
   - the session lifecycle (log-in, CSRF, expiry, log-out) in `docs/api-contract.md` and README.md;
   - the I1 status, results and differences from the plan in `docs/implementation-plan.md`;
   - PD02 in `docs/constraints.md`, once the user has decided it.
6. **Report** as in AGENTS.md §9. Don't commit. Wait for the user to ask for it.

## Validation

1. **Automated tests** (back end, against PostgreSQL with Testcontainers):
   - Valid credentials log the user in, and the profile returns that user.
   - Invalid credentials give `401` "Invalid email or password.". An unknown email and a wrong password give exactly the same status, body and headers.
   - Requests without authentication give `401` "Please log in to continue.".
   - After log-out, the old session cookie gives `401`.
   - The session id changes at log-in (protection against session fixation).
   - A request that changes data without a valid CSRF token gives `403`.
   - Access with two different users:
     - Ana (manager) and Rui (member) can access *Website*;
     - Sara (not a member) gets `404` "Project not found.";
     - Rui gets `403` on a manager-only action in *Website*.
   - No password, password hash or session id appears in the logs.
2. **Browser application tests** (`npm test`): the log-in form, the redirect on `401` with "Please log in to continue.", and log-out.
3. **Full build:** `./mvnw verify` passes, including `ModularityTest` and the front-end tests. If `openapi.yaml` changes, it still validates.
4. **Manual check** with the application running (use `SERVER_PORT=8081` if port 8080 is in use): log in as Ana and as Sara in the browser, check what each one can access, log out, and check the redirect.
5. **Report** each check and its result. Say what couldn't be checked.
