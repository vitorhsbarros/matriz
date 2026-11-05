# Matriz — Architecture

This is the entry point of the Architecture Pack of the Matriz MVP. It summarises the chosen architecture and why it was chosen. The details are in the documents linked from each section.

## 1. Decision

Matriz is a **modular monolith** ([ADR-0001](architecture/adr/ADR-0001-modular-monolith.md)):
- **One application and one database:** a single application, deployed as one unit, with one relational database.
- **Four business modules:** Accounts, Projects, Teams and Tasks. Each module owns its own data and exposes a small public interface to the others.
- **Simple layers inside each module:** presentation, business logic and data access.
- **One-way dependencies:** Projects and Teams depend on Accounts, and Tasks depends on Projects and Teams. When Projects removes a member or deletes a project, or Teams deletes a team, Tasks reacts to an in-process event, in the same transaction (D10, FR05, FR20).
- **Sessions:** server-side sessions identified by a session cookie, stored in the shared database, with a CSRF token on every data-changing request ([ADR-0002](architecture/adr/ADR-0002-session-cookie-authentication.md)).

```mermaid
flowchart LR
    user([User - browser application])
    subgraph app["Matriz application (one deployable)"]
        entry["Web entry<br/>session and CSRF check"]
        acc[Accounts]
        prj[Projects]
        tms[Teams]
        tsk[Tasks]
    end
    db[("Relational database<br/>one data area per module")]
    user -- "JSON API (openapi.yaml)" --> entry
    entry --> acc & prj & tms & tsk
    prj --> acc
    tms --> acc
    tsk --> prj
    tsk --> tms
    prj -.->|"member removed (D10),<br/>project deleted (FR05)"| tsk
    tms -.->|"team deleted (FR20)"| tsk
    acc & prj & tms & tsk --> db
```

## 2. Justification

**Why this architecture suits the MVP** (ADR-0001). The context is one developer, about 3 weeks, about 50 users and a local demonstration (constraints.md, ASM07–ASM10).

| Alternative | Decision | Reason |
|---|---|---|
| **Monolith (layers only)** | Rejected | Same effort to run and demonstrate as the chosen option, but the business areas aren't visible in the code, and changes in one area can break another. |
| **Modular monolith, simple layers** | **Chosen** | The same single application and database as the monolith, so the demo is just as simple. Business boundaries are visible and easy to defend. Changes stay contained. A module can later be extracted if it ever needs to scale on its own. |
| **Modular monolith with Onion/Clean inside each module** | Rejected | An estimated 30–50% more code and time for an application that is mostly CRUD with few rules. It adds no scalability. |
| **Microservices** | Rejected | Several services, databases and network calls, and a riskier demo, with no requirement that needs them. The rules that cross areas (NFR03, D09, D10) would lose their shared transaction. |

**Why server-side sessions with a cookie** (ADR-0002):
- **Log-out:** it is immediate (US02 AC02).
- **Security:** the cookie can't be read by page scripts.
- **No extra infrastructure:** sessions are stored in the shared database.
- **Effort:** it is the lowest-effort option for the context.

The full comparison is in [03-alternatives.md](architecture/03-alternatives.md).

## 3. Contents of the Architecture Pack

| Document | Contents |
|---|---|
| [architecture/02-domain-model.md](architecture/02-domain-model.md) | Domain model at two levels: business areas and conceptual model, with the invariants and the task life cycle. |
| [architecture/03-alternatives.md](architecture/03-alternatives.md) | Monolith, modular monolith and microservices compared for the MVP. |
| [architecture/04-solution-architecture.md](architecture/04-solution-architecture.md) | Containers, modules and their responsibilities, dependency rules, key sequences (US05, US09, US13), quality attributes, deployment view, risks and open points. |
| [architecture/adr/](architecture/adr/) | Architecture decision records: ADR-0001 (modular monolith) and ADR-0002 (session cookie authentication). |
| [database.md](database.md) | Logical data model and ER diagram. |
| [../openapi.yaml](../openapi.yaml) and [api-contract.md](api-contract.md) | API specification (24 operations) and the reasoning behind it. |
| [ui/](ui/README.md) | Low-fidelity wireframe of My projects and the project board. |

## 4. Assumptions and pending decisions

- **Assumptions:** the context assumptions behind the architecture (ASM07–ASM11) are in [constraints.md, section 2](constraints.md#2-assumptions). Assumptions specific to one artefact are recorded where they apply:
  - the data model (A1–A5);
  - the API contract (AS01–AS15).
- **Pending decisions:** listed in [constraints.md, section 4](constraints.md#4-pending-decisions). The ones that affect the architecture most are:
  - the technology stack (PD01);
  - session expiry (PD02);
  - the tool for checking module boundaries (PD03);
  - how references between modules' data are protected (PD04);
  - concurrent assignment and removal (PD05).
