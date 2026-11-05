## Goal
Act as the API designer for Matriz. Document the REST operations of the MVP as an OpenAPI 3.1 specification. The assignment asks for at least two operations; document every operation the MVP user stories need.

## Context
- **Sources:**
  - the user stories in `docs/user-stories/`, including the decisions D01–D12 and the exact messages in the acceptance criteria;
  - the domain model in `docs/architecture/02-domain-model.md`;
  - the data model in `docs/database.md`;
  - the architecture in `docs/architecture/adr/ADR-0001-modular-monolith.md`.
- **Interface:** assume the API is consumed by a client application running in the browser (e.g. a single-page application). Record this as an assumption, since the architecture left the choice between server-rendered pages and a separate client open.
- **Authentication:** session cookie. Sessions are kept on the server, in the shared database (the SESSION entity in the data model), so log-out invalidates them immediately (US02 AC02) and any instance can serve any request (ADR-0001). The cookie is `HttpOnly`, `Secure` and `SameSite`. Operations that change data are protected against CSRF.

## Constraints
- **Errors:** use the standard Problem Details format (RFC 9457, media type `application/problem+json`). The `detail` field carries the exact message from the user story's acceptance criteria (e.g. "Task title is required.").
- **Conventions:**
  - field names in `camelCase` (e.g. `dueDate`);
  - URLs with plural resource names, nested where a resource belongs to another (e.g. `/projects/{projectId}/tasks`).
- **Scope:** every operation needed by the MVP user stories (US01–US13), and only those. Don't anticipate the Should requirements.
- **Language:** everything in English (C06).
- **Ambiguities:** assume the simplest reasonable option and record it as an assumption.

## Task
1. Identify the operations needed by each MVP user story.
2. Define their HTTP methods and endpoints.
3. Identify parameters, request and response schemas, and the main status codes.
4. Generate an OpenAPI 3.1 specification in YAML and save it in `openapi.yaml`, at the repository root.
5. Write a short supporting document in `docs/api-contract.md` with:
   - the operations and the main design decisions;
   - how each one maps to its user stories and acceptance criteria;
   - the assumptions made, including the client application and session cookie authentication.

## Validation
Before delivering, check that:
- every error in the acceptance criteria of the MVP user stories has a status code and the exact message in `detail`;
- field limits (length, required or optional, enum values) match `docs/database.md`;
- the conventions are followed: `camelCase` fields, plural and nested URLs, Problem Details errors;
- the YAML is a valid OpenAPI 3.1 document. Check it with a validation tool if one is available; otherwise review it manually and say so.
