# ADR-0001 — Modular monolith with simple layers inside each module

## Status

Accepted, 2026-11-04.

## Context

Matriz is a much-simplified project and task management application. The MVP covers 13 user stories (US01–US13) implementing FR01–FR03, FR06–FR11, FR13–FR15 and NFR02–NFR04, NFR07, and must comply with constraints C01–C08.

The drivers for this decision are:

- **Team and timeline:** one developer, experienced in Java and Spring Boot, with **3 weeks** (about 15 working days) for 13 user stories, i.e. about one story per day including tests.
- **Scale:** about 50 users, equivalent to 2 or 3 pilot teams. No requirement asks for scale; NFR06 (Should) is about response time for up to 500 tasks, not user volume.
- **Deployment:** the application runs locally and is demonstrated on a server, which can be the developer's own machine.
- **Academic context:** no architectural style is required; the architecture must be explainable and defensible in a report. Demonstrating distributed systems is not a goal of the course.
- **Rules that cross feature areas:** every task operation checks project membership (NFR03, DoD03); removing a project member unassigns that person's tasks in the project (D10, US09 AC03); a task can only be assigned to a team whose members all belong to the project (D09, US13 AC04); people are added by email and shown by name (D04, D05); changes are visible on the next read and the last saved change wins (NFR07, D11).
- **Nature of the domain:** mostly create, read, update and list operations with a small number of business rules. No requirement asks to replace the database or the user interface.
- **Evolution:** the Should requirements (FR04, FR05, FR12, FR16, FR17, FR18, NFR05, NFR06) come next.

Three alternatives were compared in [03-alternatives.md](../03-alternatives.md): A (monolith), B (modular monolith) and C (microservices). A variant of B with Onion/Clean architecture inside each module was also considered.

## Decision

Matriz is built as a **modular monolith** (Alternative B):

1. **One deployable application and one shared relational database.**
2. **Four business modules:** Accounts, Projects, Teams and Tasks. Each has a **public interface** that other modules may use and an **internal part** that they may not. Each module **owns its own data** inside the shared database; no module reads or writes another module's data directly.
3. **Dependencies between modules point in one direction:** Projects → Accounts, Teams → Accounts, Tasks → Projects and Teams. There are no cycles.
4. **D10 is handled with an in-process internal event**, "member removed", published by Projects and handled by Tasks **in the same transaction**. This lets Projects trigger the unassignment without depending on Tasks, which avoids a dependency cycle.
5. **Inside each module, simple layers:** presentation → business logic → data access. This is deliberately **not** Onion, Clean or Hexagonal architecture.
6. **Sessions are not held in the memory of a single application instance**, so several identical instances can run at the same time behind a load balancer if ever needed.

## Alternatives considered

### A — Monolith

Rejected. In the user's terms:

- B has the **same runtime, deployment and demo risk** as A: one application and one database, started on a local server. A has no advantage in running or demonstrating it.
- In B, the business boundaries (accounts, projects, teams, tasks) are **visible in the code and defensible in an academic report**, not just "layered".
- In B, **changes stay contained in a module**, and most Should requirements fall inside one module (FR04, FR05, FR12, FR17 in a single module; FR18 and FR16 use one public interface).
- Clear module boundaries are **the path to scaling a single part independently later**, by extracting a module into a service. A doesn't offer that.
- The **extra cost over A is small at MVP size**: the public interfaces and the internal event for D10.

### C — Microservices

Rejected. In the user's terms:

- With about 50 users, one developer, 3 weeks and a local demo, the costs of a distributed system (several deployables and databases, network calls, a gateway, higher demo risk) **aren't justified by any requirement**.
- The rules that cross areas (NFR03, D09, D10) would need **coordination across services without a shared transaction**.
- **Demonstrating distributed systems is not a goal** of the course.

### B with Onion/Clean architecture inside each module

Considered explicitly and rejected. In the user's terms:

- With 3 weeks for 13 user stories (about one story per day, including tests), Onion/Clean adds an **estimated 30–50% more code and time**: ports, adapters, separate domain and persistence models, and mappers between them.
- Matriz is **mostly CRUD with few business rules**, and **no requirement asks to swap the database or the interface**. Onion/Clean pays off when there is a lot of domain logic to protect, which isn't the case here.
- Clean **wouldn't add scalability**. Scalability comes from module boundaries and instance-independent sessions, both part of this decision.
- Onion/Clean **stays open as a future evolution for a specific module** if its rules grow. It doesn't need to be adopted everywhere now.

## Consequences

### Positive

- **Simple demo:** one application and one database on the developer's machine.
- **Consistent cross-module rules:** NFR03, D09 and D10 run in-process, in a single transaction. D10 in particular either fully happens (member removed and their tasks unassigned) or doesn't happen at all.
- **Contained changes:** each module has one clear responsibility, and most Should requirements stay inside one module.
- **A path to scaling**, although no requirement asks for it today:
  - *Horizontal scaling of the whole application:* several identical instances share one database. Instance-independent sessions are the condition for this, and they are part of the decision.
  - *Independent scaling of one part:* extract that module into a service, which the module boundaries make possible.
- **Low code overhead inside modules:** simple layers fit the one-story-per-day pace.

### Negative

- **Boundaries need discipline and an automated check.** Without it, modules can start using each other's internals or data. The checking tool is chosen in the stack phase.
- **Some extra code to cross boundaries**, e.g. the task list must fetch people's names (from Accounts, through the Projects member list) and team names (from Teams) instead of reading their data directly. This also needs care for NFR06 (Should) later, by fetching names in batches.
- **The module split may need adjusting** as the domain becomes clearer. At MVP size this is cheap.
- **Simple layers give less isolation** between business rules and data access than Onion/Clean. If one module's rules grow, it may need refactoring to a richer internal structure.
- **Instance-independent sessions** add a small amount of work compared with keeping them in memory.
