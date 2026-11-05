# ADR-0002 — Server-side sessions with a session cookie, stored in the shared database

## Status

Accepted, 2026-11-04.

## Context

Users sign up and log in with email and password, and every other operation requires being logged in (FR01, FR02, NFR02, DoD02, US01, US02). [ADR-0001](ADR-0001-modular-monolith.md) left one condition on how this works: **sessions must not be held in the memory of a single application instance**, so that any instance can serve any request. The concrete mechanism was left open as the "Session storage" risk in [04-solution-architecture.md, section 7](../04-solution-architecture.md#7-known-risks-and-open-points), and as open point OP2 in the data model (`docs/database.md`).

The drivers for this decision are:

- **Immediate log-out:** after logging out, the user's access must stop at once; any further request asks them to log in again (US02 AC02, US02 AC06).
- **Access only when logged in:** every protected request must be checked (NFR02, DoD02).
- **Security in the browser:** the only client is a browser application (assumption AS01 in `docs/api-contract.md`; AS01 in [03-alternatives.md](../03-alternatives.md)). Credentials kept by the browser must be protected from scripts in the page, and the chosen mechanism must not open the application to cross-site request forgery.
- **No external identity provider:** login is by email and password only (C03; AS03 in 03-alternatives.md).
- **Team and timeline:** one developer and 3 weeks for 13 user stories (ADR-0001).
- **Instance independence and no extra infrastructure:** the modular monolith runs as one deployable with one shared relational database (ADR-0001).

The API contract (`docs/api-contract.md`, section 4 and assumption AS02) was written assuming this mechanism and suggested recording it as ADR-0002.

## Decision

Authentication in Matriz uses **server-side sessions identified by a session cookie**, not self-contained tokens such as JWT.

1. **Mechanism:** sign-up and log-in create a session on the server and set a session cookie in the browser. The cookie carries only an unguessable session identifier.
2. **Storage:** sessions are stored in the **shared relational database**, as the SESSION entity **owned by the Accounts module**. They are never held in an instance's memory, so any instance can serve any request (ADR-0001, decision 6).
3. **Cookie attributes:** `HttpOnly`, `Secure` and `SameSite=Lax`.
4. **CSRF protection:** every operation that **changes data while logged in** requires a **CSRF token sent in a request header**. The token is issued together with the session. Sign-up and log-in don't require it, because no session exists yet.
5. **Log-out:** deletes the session on the server, so the cookie stops working immediately (US02 AC02).
6. **Session check:** the web entry asks Accounts to resolve the session (cookie → current user or none) before every protected request, as already described in 04-solution-architecture.md, section 3.3 (NFR02).

**Not decided here:** the session expiry rule. It stays open (04-solution-architecture.md, section 7, open point 2); until it is decided, sessions last until log-out.

## Alternatives considered

### Self-contained tokens (e.g. JWT) kept by the browser

Rejected. In the user's terms:

- **Log-out:** a token stays valid until it expires unless a revocation list is kept. That list would itself be shared server-side state, which removes most of the benefit and makes US02 AC02 harder. Server-side sessions make log-out immediate and simple.
- **Security in the browser:** a token kept where page scripts can read it is exposed to any XSS bug. An `HttpOnly` cookie can't be read by scripts.
- **Need:** tokens pay off with several kinds of clients, other services or public APIs. The MVP has one browser client (AS01) and no other services (ADR-0001, C03).
- **Effort:** more to build and get right (signing, key handling, revocation) for a single developer with 3 weeks.

### Server-side sessions kept in the memory of each instance

Rejected by ADR-0001 (decision 6): a session would only be known to the instance that created it, so several identical instances couldn't serve the same user.

### Server-side sessions in a separate shared store (e.g. a dedicated cache)

Rejected. It works and satisfies ADR-0001, but it **adds a runtime component** to build, run and demonstrate that the MVP doesn't need. With about 50 users, the shared database already meets the requirement with no extra infrastructure.

## Consequences

### Positive

- **Immediate log-out** by deleting the session row (US02 AC02, AC06).
- **Instance-independent sessions with no extra infrastructure:** the shared database is already part of ADR-0001, so the horizontal-scaling option ADR-0001 keeps open stays open.
- **Session credential not readable by page scripts** (`HttpOnly`), which limits the damage of an XSS bug.
- **Low effort:** server-side sessions are the default option and the lowest-effort one for one developer with 3 weeks.
- **Clear ownership:** sessions are Accounts data (ADR-0001, rule of data ownership); other modules only receive the current user's identifier from the web entry.
- **Settles** the "Session storage" risk in 04-solution-architecture.md and open point OP2 of the data model (SESSION exists, in the database).

### Negative

- **CSRF exposure**, the cost of using a cookie. It is handled by `SameSite=Lax` plus the CSRF token on every data-changing operation, which every such operation in the API must check.
- **A database read on every protected request** to resolve the session. Acceptable at about 50 users; it is one more item to watch if NFR06 (Should) or a scalability NFR is added.
- **Same-site deployment of client and API:** with `SameSite=Lax`, the browser only sends the cookie on requests to the same site. The browser client and the API must be served from the same site (e.g. the same origin), which the deployment must respect.
- **`Secure` requires HTTPS** outside the local machine. Browsers generally accept `Secure` cookies on `localhost`, but a demonstration reached over plain HTTP from another machine would not keep the session. The demo environment must provide HTTPS or be accessed locally.
- **Expired sessions are not removed yet:** without an expiry rule (open point 2), sessions that are never logged out stay in the database indefinitely.
- **Tied to browser clients:** if other client types are added later (e.g. mobile), a token-based mechanism may be needed alongside the cookie. That would be a new ADR.
