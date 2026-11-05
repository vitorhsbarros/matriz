# Matriz — Architecture Pack

Index and status of the architecture documents. At the user's request, this is a reduced pack: the drivers document (`01`) is not produced.

## Documents

| Document | Status |
|---|---|
| [02-domain-model.md](02-domain-model.md) | Complete. Domain model of the MVP at two levels: business areas and conceptual model. |
| [03-alternatives.md](03-alternatives.md) | Complete. Alternative B was chosen. Timeline updated to 3 weeks. |
| [04-solution-architecture.md](04-solution-architecture.md) | Draft for the chosen architecture: modular monolith with simple layers inside each module. Updated for ADR-0002 (sessions); the "Session storage" risk is resolved. |
| [adr/ADR-0001-modular-monolith.md](adr/ADR-0001-modular-monolith.md) | Accepted |
| [adr/ADR-0002-session-cookie-authentication.md](adr/ADR-0002-session-cookie-authentication.md) | Accepted |

## Decisions

| ADR | Decision | Status |
|---|---|---|
| [ADR-0001](adr/ADR-0001-modular-monolith.md) | Modular monolith (Accounts, Projects, Teams, Tasks) with one-way dependencies, an in-process event for D10, simple layers inside each module and sessions independent of any instance | Accepted, 2026-11-04 |
| [ADR-0002](adr/ADR-0002-session-cookie-authentication.md) | Server-side sessions identified by a session cookie (`HttpOnly`, `Secure`, `SameSite=Lax`), stored in the shared database (Accounts), CSRF token in a request header for data-changing operations, log-out deletes the session. Expiry left open. | Accepted, 2026-11-04 |

## Open points

Listed in [04-solution-architecture.md, section 7](04-solution-architecture.md#7-known-risks-and-open-points): FR16 vs NFR03 (resolved in requirements.md, review-01 F03), session expiry (still open after ADR-0002), the tool for checking module boundaries, and scalability as a possible NFR.
