## Goal
Act as a critical reviewer of the Matriz artefacts. Identify omissions and contradictions between the requirements, the architecture, the data model, the API and the wireframe, so that the relevant problems can be corrected. No change is accepted automatically: each correction is decided by me.

## Context
- **Artefacts to review:**
  - requirements (`docs/requirements-v2.md`), constraints (`docs/constraints.md`) and user stories (`docs/user-stories/`);
  - architecture (`docs/architecture/`, including ADR-0001 and ADR-0002) and the domain model (`docs/architecture/02-domain-model.md`);
  - data model (`docs/database.md`);
  - API (`openapi.yaml` and `docs/api-contract.md`);
  - wireframe (`docs/ui/`).
- **Out of the review:** the v1 requirements (`docs/requirements.md`, kept for history) and the prompts (`docs/prompts/`).
- **Source of truth:** the user stories, the constraints and the accepted ADRs. When another artefact contradicts them, that artefact is the one to correct.

## Constraints
- **Human in the loop:**
  - the assistant only reads, reports and proposes the exact text of each correction;
  - it doesn't change any artefact on its own.
- **Decisions:**
  - I decide on each problem individually: accept, reject or adjust the proposed correction;
  - only the corrections I accept, or adjust, are applied.
- **Severity:** every problem is reported, with one of three levels:
  - **High:** a contradiction that would lead to a wrong implementation;
  - **Medium:** an omission or an outdated artefact;
  - **Low:** wording or a broken reference.
- **Source of truth:** problems found in the user stories, the constraints or the accepted ADRs are only flagged, with no proposed correction, because the decision is mine.
- **Language:** everything in English (C06).

## Task
1. Read every artefact in scope.
2. Cross-check them, pair by pair, and list every omission or contradiction:
   - user stories ↔ requirements;
   - user stories ↔ data model and domain model;
   - user stories ↔ API, including the exact messages;
   - API ↔ data model, including fields and limits;
   - user stories ↔ wireframe;
   - ADRs ↔ architecture, data model and API.
3. Write the findings in `docs/reviews/review-01.md`, as a table with:
   - ID;
   - severity;
   - artefacts involved;
   - exact location (file and section);
   - description;
   - proposed correction (exact text);
   - my decision and its reason.
4. Stop and wait for my decisions.
5. Apply only the corrections I accept or adjust, and record each decision and its reason in the report.
6. Commit the approved corrections together with the report.

## Validation
- **Before presenting the report, check that:**
  - every finding has a severity, an exact location and a proposed correction, except findings in the source of truth, which are only flagged;
  - every cross-check listed in the Task was done.
- **Before committing, check that:**
  - only the approved corrections were applied;
  - every decision has its reason recorded in the report;
  - the corrections didn't introduce new contradictions;
  - `openapi.yaml` is still a valid OpenAPI 3.1 document, if it was changed.
