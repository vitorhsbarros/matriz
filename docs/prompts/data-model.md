## Goal
Act as the data modeller for Matriz. Produce the data model and the ER diagram that represent the essential data of the MVP, to document the architecture and to serve as the basis for implementation.

## Context
- **Sources:**
  - the MVP scope in section 5 of `docs/requirements-v2.md`;
  - the user stories in `docs/user-stories/`, including the decisions D01–D12;
  - the constraints in `docs/constraints.md`.
- **Architecture:** the model must follow ADR-0001 (`docs/architecture/adr/ADR-0001-modular-monolith.md`). Matriz is a modular monolith with a single relational database. Its data is split across four modules (Accounts, Projects, Teams, Tasks). Each module owns its own data and only stores the identifiers of other modules' data.
- **Open point:** whether references between modules' data are protected by rules in the database (foreign keys) or only stored as identifiers is still undecided (`docs/architecture/04-solution-architecture.md`). Take it into account: show those references clearly in the model and flag the choice as a point to confirm, without deciding it.

## Constraints
- **Model level:** logical model, with entities, attributes, identifiers, keys, relationships, cardinalities and generic data types (e.g. text, integer, date, timestamp). Don't name a specific database technology or its data types (C07).
- **Scope:** only the data needed by the MVP (the Must requirements and the MVP user stories).
- **Size:** keep the document short: the ER diagram plus one concise table per entity.
- **Language:** write in English (C06).
- **Ambiguities:** assume the simplest reasonable option and flag it as an assumption to confirm.

## Task
1. Analyse the MVP requirements and identify entities, attributes, identifiers and relationships.
2. Define cardinalities and flag business rules or assumptions to confirm.
3. Generate the ER diagram in Mermaid (`erDiagram`) and review its consistency.
4. Save the short model and the Mermaid code in `docs/database.md`, containing:
   - the ER diagram;
   - one concise table per entity (attributes, generic types, keys, constraints);
   - a map of which entities belong to which module (Accounts, Projects, Teams, Tasks);
   - the business rules the model supports, traced to user stories and decisions (D01–D12);
   - the assumptions and open points to confirm.

## Validation
Before delivering, check that:
- every MVP user story (US01–US13) is supported by the model;
- no entity or attribute is unused, meaning each one is needed by at least one user story or rule;
- cardinalities match the business rules (e.g. a task has at most one assignee);
- the ER diagram and the entity tables are consistent with each other;
- each entity belongs to exactly one module.
