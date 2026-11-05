# Review 01 — Consistency of the Matriz artefacts

- **Date:** 2026-11-04
- **Reviewer:** Claude, acting as critical reviewer. The prompt is in [docs/prompts/critical-review.md](../prompts/critical-review.md).
- **Decisions:** Vitor Barros (human in the loop).
- **Note:** the paths below are those at the time of the review. The documents were later reorganised: `docs/requirements-v2.md` became `docs/requirements.md`, the v1 became `docs/requirements-v1.md`, the requirements' assumptions moved to `docs/constraints.md` (ASM01–ASM06), and `docs/user-stories/` was merged into `docs/user-stories.md`.
- **Status:** decisions taken and approved corrections applied. F05, S01 and S02 had no decision and remain open.

## Scope

**Reviewed:**
- requirements: `docs/requirements-v2.md`, `docs/constraints.md` and `docs/user-stories/`;
- architecture: `docs/architecture/`, including ADR-0001, ADR-0002 and the domain model;
- data model: `docs/database.md`;
- API: `openapi.yaml` and `docs/api-contract.md`;
- wireframe: `docs/ui/`.

**Out of scope:** `docs/requirements.md` (v1, historical) and `docs/prompts/`.

**Source of truth:** the user stories, the constraints and the accepted ADRs. Problems found in them are only flagged, without a proposed correction.

**Severity:**
- **High:** a contradiction that would lead to a wrong implementation.
- **Medium:** an omission or an outdated artefact.
- **Low:** wording or a broken reference.

## Cross-checks performed

| Cross-check | Result |
|---|---|
| User stories ↔ requirements | F03, F06, F07 |
| User stories ↔ data model and domain model | No problem found. Every MVP story is covered (database.md section 5), and the rules match D01–D12. |
| User stories ↔ API, including exact messages | F01. Every acceptance criteria message appears in `openapi.yaml`. |
| API ↔ data model, including fields and limits | F04, F05. Field names, limits, enums and optional fields match. |
| User stories ↔ wireframe | S02. Elements, messages and statuses match. The omitted screens are documented in `docs/ui/README.md`. |
| ADRs ↔ architecture, data model and API | F02, F08, S01. ADR-0001 and ADR-0002 are reflected in 04-solution-architecture, database.md and the API. |

## Findings

| ID | Severity | Artefacts | Location | Description | Proposed correction | Decision and reason |
|---|---|---|---|---|---|---|
| F01 | **High** | API ↔ user stories | `openapi.yaml`: `info.description`, `components.responses.MalformedRequest`. `docs/api-contract.md`: section 2 "Status codes", AS07 | The schemas declare `required`, `minLength`, `maxLength`, `format` and `enum`, and `400` is described as "doesn't match the expected structure". A generic validator would therefore answer `400` "The request body is not valid." where the acceptance criteria require `422` with a specific message: an empty title (US05 AC03), a non-existent date (US05 AC05), status "Blocked" (US07 AC04), an invalid email (US01 AC05) or missing log-in fields (US02 AC05). | Define `400` narrowly and say that value rules return `422`. Exact text in [F01](#f01). | **Accepted** as proposed. Applied. |
| F02 | **Medium** | ADR-0002, API ↔ architecture | `docs/architecture/03-alternatives.md` AS05. `docs/architecture/04-solution-architecture.md` section 2 (Browser row) and section 3.1 (Presentation) | 03-alternatives AS05 leaves open whether the interface is rendered by the server or is a browser application calling an API. Section 3.1 of 04 still says each module "shows its screens". But the API (api-contract AS01) and ADR-0002 (same-site deployment) assume a browser application that calls the JSON API in `openapi.yaml`. The architecture doesn't record this choice. | Update 04 so it describes the browser application and the API. Exact text in [F02](#f02). An alternative is to record the choice as ADR-0003. | **Accepted** as proposed (update 04, no ADR-0003). Applied. |
| F03 | **Medium** | Requirements ↔ ADR-0001, user stories | `docs/requirements-v2.md` section 3, FR16 | FR16 shows users the tasks assigned to their teams. Under D09 (US12 AC08), a team can include people who aren't in the project. FR16 would then show them tasks of a project they can't access, which NFR03 and DoD03 forbid. This is open point 1 in 04-solution-architecture. | Restrict FR16 to projects the user is a member of. Exact text in [F03](#f03). | **Accepted** as proposed. Applied. |
| F04 | Low | API | `openapi.yaml`: `createTeam` → `201`, `createTask` → `201` | Both responses return a `Location` header, but there is no `GET /teams/{teamId}` and no `GET /projects/{projectId}/tasks/{taskId}` (api-contract AS14). The header points to a URL that can't be read. | Remove the `Location` header from these two responses. Exact text in [F04](#f04). | **Accepted**: remove the headers. Applied. |
| F05 | Low | Architecture ↔ data model | `docs/architecture/04-solution-architecture.md` section 3.4, Projects → **Owns** | It says Projects owns "memberships (project, user, role)". The data model doesn't store the role: it is derived from the project's manager (database.md, A3). The domain model and the API are consistent with the data model. | Align the wording with the data model. Exact text in [F05](#f05). | **No decision.** Not applied. |
| F06 | Low | Requirements ↔ user stories | `docs/requirements-v2.md` section 3, FR15 | FR15 says the system must "list" a project's tasks. Since US06 changed, the tasks are shown as a board with one column per status. The intent still holds, but the wording no longer describes the agreed interface. | Mention the board in FR15. Exact text in [F06](#f06). | **Accepted**: mention the board. Applied. |
| F07 | Low | Requirements ↔ user stories | `docs/requirements-v2.md` section 7, assumption 5 | The assumption says a task can only be assigned to a team whose members all belong to the project. It doesn't say that this is checked only at the moment of assignment (D09, US13 AC07). Read alone, it suggests the rule must always hold. | Add the timing from D09. Exact text in [F07](#f07). | **Accepted** as proposed. Applied. |
| F08 | Low | ADR-0002 ↔ API | `openapi.yaml`: `servers[0]` | The server URL is `http://localhost:8080/api`, while the session cookie is `Secure`. ADR-0002 notes that this only works when the page is opened locally. The specification doesn't say so, and the demo could be opened from another machine over HTTP, which would lose the session. | Add the condition to the server description. Exact text in [F08](#f08). | **Accepted** as proposed. Applied. |
| S01 | Medium | Source of truth: US02, ADR-0002 | `docs/user-stories/US02-log-in-and-out.md`. ADR-0002, "Not decided here" | **Flagged only.** There is no session expiry rule. Sessions last until log-out, and abandoned sessions stay in the database indefinitely (ADR-0002, negative consequences). | — (your decision: e.g. a new acceptance criterion in US02 and an ADR, or keep it open) | **No decision.** Stays open. |
| S02 | Low | Source of truth: US06 | `docs/user-stories/US06-list-project-tasks.md` title. `docs/user-stories/README.md` backlog row 6 | **Flagged only.** The title "List a project's tasks" no longer matches the board described in US06's rules and AC01. | — (your decision: e.g. rename to "View a project's task board") | **No decision.** Stays open. |

## Proposed corrections

### F01

**`openapi.yaml`, `info.description`.** Replace the paragraph that starts with "**Errors.**" with:

> **Errors.** Errors use Problem Details (RFC 9457). The `detail` field carries the exact message defined in the user stories' acceptance criteria. When several validations fail, only the first one is reported, in the order the fields are listed in the request schema.
>
> **Validation.** `400` is only returned when the body isn't valid JSON, has unknown fields, or has a field of the wrong JSON type (e.g. a number where text is expected). Missing or empty required fields, lengths, formats (email, date) and enum values are business validations: they return `422` with the message from the acceptance criteria. The schema keywords `required`, `minLength`, `maxLength`, `format` and `enum` document these rules, and must not be enforced by a generic validator that returns `400`.

**`openapi.yaml`, `components.responses.MalformedRequest.description`.** Replace with:

> The request body isn't valid JSON, has unknown fields, or has a field of the wrong JSON type. Missing, empty or out-of-range values return `422` instead.

**`docs/api-contract.md`, section 2 "Status codes".** Replace the `400` and `422` rows with:

> | `400` | Body isn't valid JSON, has unknown fields, or has a field of the wrong JSON type |
> | `422` | Valid structure, but a field is missing or empty, or a value, format, enum or business rule fails |

**`docs/api-contract.md`, AS07.** Replace the assumption text with:

> A body that isn't valid JSON, has unknown fields or has a field of the wrong JSON type returns `400` with "The request body is not valid." Missing or empty fields, lengths, formats and enum values return `422` with the acceptance criteria message (e.g. US05 AC03, AC05, US07 AC04).

### F02

**`docs/architecture/04-solution-architecture.md`, section 2, Browser row.** Replace the responsibility with:

> Runs the client application: a browser application served by the Matriz application from the same site (ADR-0002), which calls the API described in `openapi.yaml`. Shows the interface and the exact English messages returned by the API. Holds no business rules. Keeps the session cookie (`HttpOnly`, so page scripts can't read it) and sends the CSRF token in a request header on every data-changing operation.

**Same file, section 3.1, Presentation row.** Replace with:

> Exposes the module's API operations (`openapi.yaml`). Receives the requests, returns the data and the exact English messages (C05, DoD01, DoD04) in Problem Details errors. Calls only its own business logic.

**Same file, after the table in section 2.** Add:

> The browser application calling a JSON API settles assumption AS05 of [03-alternatives.md](03-alternatives.md) (see AS01 in `docs/api-contract.md`).

### F03

**`docs/requirements-v2.md`, FR16.** Replace the requirement text with:

> The system must list the tasks assigned to the user, including those assigned to teams they belong to, only in projects the user is a member of.

**`docs/architecture/04-solution-architecture.md`, section 7, open point 1.** Append:

> Resolved in requirements-v2 (FR16 is limited to projects the user is a member of).

### F04

**`openapi.yaml`.** Delete these lines from `createTeam` → `responses.'201'` and from `createTask` → `responses.'201'`:

```yaml
          headers:
            Location:
              $ref: '#/components/headers/Location'
```

### F05

**`docs/architecture/04-solution-architecture.md`, section 3.4, Projects, Owns.** Replace with:

> Projects (name, description, manager), memberships (project, user). A member's role is derived: "Manager" for the project's manager, "Member" otherwise (database.md, A3).

**Same file, section 3.4, Teams, Owns.** Replace with:

> Teams (unique name, manager), team memberships (team, user). The role is derived the same way.

### F06

**`docs/requirements-v2.md`, FR15.** Replace the requirement text with:

> The system must show a project's tasks as a board with one column per status, showing the assignee of each task.

### F07

**`docs/requirements-v2.md`, section 7, assumption 5.** Replace the last sentence with:

> A task can only be assigned to a team whose members all belong to the project. This is checked only when the task is assigned; later changes to the team don't undo the assignment (D09).

### F08

**`openapi.yaml`, `servers[0].description`.** Replace with:

> Local server (development and demonstration). The session cookie is `Secure`: over plain HTTP it only works when the page is opened on the same machine (`localhost`). Reached from another machine, the server must use HTTPS (ADR-0002).

## Decision log

Decisions taken by Vitor Barros on 2026-11-04. No reasons were given beyond the decision itself, so none are recorded.

| ID | Decision | Changes applied |
|---|---|---|
| F01 | Accepted | `openapi.yaml`: new "Validation" paragraph in `info.description`, and the `MalformedRequest` description. `docs/api-contract.md`: the `400` and `422` rows in section 2, and AS07. |
| F02 | Accepted (update 04, no ADR-0003) | `docs/architecture/04-solution-architecture.md`: the Browser row and the note after the table in section 2, and the Presentation row in section 3.1. |
| F03 | Accepted | `docs/requirements-v2.md`: FR16. `docs/architecture/04-solution-architecture.md`: section 7, open point 1. **Consequential change:** `docs/architecture/README.md`, open points, marks FR16 vs NFR03 as resolved, so the index doesn't contradict the correction. |
| F04 | Accepted | `openapi.yaml`: removed the `Location` header from the `201` responses of `createTeam` and `createTask`. |
| F05 | No decision | None. |
| F06 | Accepted (mention the board) | `docs/requirements-v2.md`: FR15. |
| F07 | Accepted | `docs/requirements-v2.md`: section 7, assumption 5. |
| F08 | Accepted | `openapi.yaml`: `servers[0].description`. |
| S01 | No decision | None. Session expiry stays open. |
| S02 | No decision | None. US06 keeps its title. |

### Checks after applying

- **Only approved corrections applied:** only F01–F04 and F06–F08 were applied, plus the consequential index update for F03 noted above.
- **`openapi.yaml`:** still a valid OpenAPI 3.1 document (Redocly CLI). The only warning is the intended `localhost` server.
- **No new contradictions:**
  - FR15 now matches US06.
  - FR16 no longer conflicts with NFR03, and the architecture marks the open point as resolved.
  - The `400`/`422` rule in the API matches DoD07 ("fields of the wrong type") and the `422` messages in the acceptance criteria.
  - 04 now matches api-contract AS01 and ADR-0002.
