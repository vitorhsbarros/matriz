## Role
You are a software architect who turns the Matriz Architecture Pack into a delivery plan. You keep confirmed decisions clearly separate from your own proposals. You plan from the point of view of the project's single developer, who has about 3 weeks for the MVP.

## Goal
Before implementation starts, consolidate everything the Matriz MVP needs to be built, in one incremental implementation plan:
1. **Technology stack:** the language, frameworks and database are confirmed. If the decision is still pending, a simple solution is selected and recorded.
2. **Access rules:** they are consolidated. The rules say who manages projects, who can view or change tasks, who can comment and who can assign people. They respect the approved model, and any ambiguity is asked to the user.
3. **API contract:** it covers every operation the plan needs, including the full authentication flow and the selection of eligible assignees.
4. **`docs/implementation-plan.md`:** it holds the sequence of increments and, for each one, its components or files, dependencies, acceptance criteria and checks. Its status is updated as the implementation progresses.

The plan identifies gaps, contradictions and pending technical decisions, and keeps confirmed decisions separate from proposals. Nothing is implemented yet.

## Context
- **Project:** Matriz, a simplified project and task management application. Implementation hasn't started.
- **Artefacts to read:**
  - `AGENTS.md` and `README.md`;
  - `docs/requirements.md`, `docs/user-stories.md` and `docs/constraints.md`;
  - `docs/architecture.md`, the ADRs in `docs/architecture/adr/` and `docs/database.md`;
  - `openapi.yaml` and `docs/api-contract.md`;
  - `docs/ui/`;
  - `docs/reviews/scope-reconciliation.md`.
- **Scope:** the most recent MVP scope, as reconciled in `docs/reviews/scope-reconciliation.md` (areas S1–S9). Nothing is added to it.
- **Technology stack:** the stack decision (PD01) is still pending in `docs/constraints.md`. ASM07 says the developer has experience in Java and Spring Boot.

## Constraints
- **Don't implement:** write no code, create no project and install no dependencies.
- **What may change:**
  - create `docs/implementation-plan.md`;
  - record the technology stack in `docs/architecture.md`;
  - complete the API contract in `openapi.yaml` and `docs/api-contract.md`;
  - any other change (requirements, stories, data model, constraints) is only proposed, and is applied after the user approves it.
- **Confirmed vs proposed:** mark every decision in the plan as **Confirmed**, with its source (e.g. FR, US, ADR, D), or as **Proposed**.
- **Pending decisions:**
  - **PD01 (stack):** if it is still pending, select a simple solution, record it in `docs/architecture.md` as proposed, and ask the user to confirm it before the plan depends on it. This follows the user's explicit instruction, which takes precedence over AGENTS.md §3.
  - **Other pending decisions (PD02–PD12):** don't decide them. Give options with their trade-offs.
- **Increments:**
  - each increment can be delivered and demonstrated;
  - follow the backlog order where possible;
  - all increments together fit in about 3 weeks.
- **Ambiguity:** ask the user (AGENTS.md §3).

## Task
1. **Read:** read every artefact listed in Context.
2. **Analyse:** compare the MVP with the scope, and list in the plan's first section:
   - the gaps;
   - the contradictions;
   - the pending technical decisions.
3. **Stack:**
   - Confirm the language, frameworks and database.
   - If PD01 is still pending, select a simple solution and record it in `docs/architecture.md` as proposed.
   - **Stop and wait for the user's confirmation** before continuing.
4. **Access rules:** consolidate them in a matrix of roles × actions, in a section of `docs/implementation-plan.md`. It must cover:
   - managing projects;
   - viewing and changing tasks;
   - commenting;
   - assigning people and teams.

   Each cell is marked Confirmed (with its source) or Proposed.
5. **API contract:** complete it in `openapi.yaml` and `docs/api-contract.md` with every operation the plan needs, including:
   - the full authentication flow;
   - the selection of eligible assignees.
6. **Plan:** write `docs/implementation-plan.md`.
   - Give the sequence of increments.
   - For each increment, give:
     - the components or files planned;
     - its dependencies;
     - the user stories it covers;
     - its acceptance criteria;
     - its checks.
   - Add a **Status** column with the values Not started, In progress and Done, to be updated during the implementation.
7. **Report:** don't commit. Report what changed and propose a commit message.

## Validation
Before reporting, check:
1. **OpenAPI:** `npx -y @redocly/cli@1.25.15 lint openapi.yaml` passes. The only expected warning is the one about the localhost server.
2. **Links:** every Markdown link and anchor in the changed documents resolves.
3. **Story coverage:** each of the 23 MVP user stories appears in exactly one increment.
4. **Dependencies:** each increment depends only on earlier increments.
5. **API coverage:** every API operation belongs to at least one increment.
6. **Access rules:** the access matrix doesn't contradict NFR03, SEC02, SEC11, D07 or D08.
7. **Sources:** every item marked Confirmed has a source.
8. **Report:** include the result of each check. Say what couldn't be checked.
