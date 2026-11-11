# Matriz

Matriz is a project and task management application for work teams, similar to Jira but much simpler. Several people can create projects, record the work as tasks with a priority, follow each task on a board (To do, In progress, Done), discuss it in comments and make clear who is responsible for it: one or more people or teams.

## Problem

The client needs an application where its teams can organise projects, manage tasks and follow the progress of the work. The project assumes these are small teams for whom tools like Jira bring sprints, reports, workflows and configuration they don't need (ASM12). Matriz keeps only what is needed to organise the work.

## Goals

1. Let a team organise its work in projects, each with its own tasks and members.
2. Make clear who is responsible for each task: one or more people or teams.
3. Show at a glance where every task stands.
4. Stay simple enough to use without training: no sprints, reports, integrations or configurable workflows.

## MVP features

| Area | What a user can do | User stories |
|---|---|---|
| **Accounts** | Log in with a demo account created by the seed, see who is logged in, and log out. | US19, US02 |
| **Home dashboard** | See their projects with the number of tasks in each status, per project and in total, and search projects by name. | US04, US23 |
| **Projects** | Create a project, open it, and (as manager) edit or delete it and add and remove members. | US03, US09, US14, US15 |
| **Teams** | Create a team and (as manager) rename or delete it and add and remove its members. | US11, US12, US17, US18 |
| **Tasks** | Create, view, edit and delete tasks with a priority, move them between To do, In progress and Done on the project's board, filter the board by status and priority, and assign several members and teams to them. | US05–US08, US10, US13, US16, US20, US24 |
| **Comments** | Add and read a task's comments, and edit or delete their own. | US21, US22 |

Public registration and password recovery are optional extensions, outside the MVP.

The full MVP scope, and what is left for later, is in [docs/requirements.md](docs/requirements.md).

## Documentation

### Specification Pack

| Document | Contents |
|---|---|
| [docs/requirements.md](docs/requirements.md) | Vision and goals, user types, functional and non-functional requirements, MVP scope, and what is out of the initial scope. |
| [docs/user-stories.md](docs/user-stories.md) | The 23 MVP user stories, with priorities, acceptance criteria and exceptional cases, and the optional extensions. |
| [docs/constraints.md](docs/constraints.md) | Known constraints, assumptions, security and privacy rules, pending technical decisions and scope limitations. |
| [AGENTS.md](AGENTS.md) | Instructions for AI coding agents working on this repository. |

### Architecture Pack

| Document | Contents |
|---|---|
| [docs/architecture.md](docs/architecture.md) | The chosen architecture and its justification, with links to the alternatives, the domain model and the decision records (ADRs). |
| [docs/database.md](docs/database.md) | Logical data model of the MVP and ER diagram. |
| [openapi.yaml](openapi.yaml) | API specification of the MVP (OpenAPI 3.1). The reasoning behind it is in [docs/api-contract.md](docs/api-contract.md). |
| [docs/ui/](docs/ui/) | Low-fidelity wireframe of the main screens, with a link to the interactive prototype. |

### Process records

| Folder | Contents |
|---|---|
| [docs/reviews/](docs/reviews/) | Critical reviews of the documentation, with the decisions taken on each finding. |
| [docs/prompts/](docs/prompts/) | The prompts used to produce the documentation with AI assistance. |
| [docs/requirements-v1.md](docs/requirements-v1.md) | First version of the requirements, kept for history. |

## Current status

- **Specification and architecture:** done.
  - The requirements, user stories, constraints, architecture, data model, API and wireframe are written.
  - The documentation was produced with AI assistance and reviewed critically, with every correction decided by a person:
    - the [specification review](docs/reviews/specification-review.md) covers the Specification Pack;
    - [review-01](docs/reviews/review-01.md) covers the Architecture Pack.
  - A [prompt injection test](docs/reviews/prompt-injection-challenge.md) checked that agents treat external documents as data.
  - The MVP was then [reconciled with a new scope](docs/reviews/scope-reconciliation.md) before implementation.
- **Implementation:** not started. The technology stack is still to be chosen (pending decision PD01 in [docs/constraints.md](docs/constraints.md)).
- **Next step:** full-stack development of the MVP, following the user stories in their implementation order.
