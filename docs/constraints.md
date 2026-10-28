# Matriz — Constraints

Constraints are fixed decisions and limits that the solution must respect. Unlike requirements, they don't describe what the application does. They limit *how far* it goes and *how* it can be specified and built.

Each requirement in [requirements-v2.md](requirements-v2.md) and each user story in [user-stories/](user-stories/) must comply with every constraint below.

## 1. Scope

The application is deliberately simple: a stripped-down alternative to Jira. These features are excluded from every version, not just postponed.

| ID | Constraint | Justification |
|---|---|---|
| C01 | The application does not support sprints or any other form of iteration planning. | Sprints are one of the main sources of complexity in tools like Jira and are not needed to organise tasks within a project. |
| C02 | The application does not produce reports, charts or statistics. | Progress is followed through task statuses (FR13, FR15). Reports would add a feature area without serving the core idea. |
| C03 | The application does not integrate with external systems (e.g. email, calendars, chat tools, third-party login, other project management tools). | Integrations add dependencies and configuration that go against keeping the application simple. |
| C04 | Features that make the application configurable (e.g. custom statuses, custom fields, custom workflows) are excluded. | Configurability is what makes tools like Jira complex. Fixed behaviour keeps the application easy to learn. This is why F06 and F07 are rated Won't. |

## 2. Language

| ID | Constraint | Justification |
|---|---|---|
| C05 | All of the application's interface and messages are in English. | Chosen language for the application. Previously NFR01. |
| C06 | All project documentation (requirements, constraints, user stories, prompts) is written in English. | Keeps the documentation consistent with the application and readable by anyone working on the project. |

## 3. Specification

| ID | Constraint | Justification |
|---|---|---|
| C07 | Requirements and user stories are technology-independent: they don't prescribe a technology stack, database, architecture or platform. | Technical decisions are made later, once the requirements are stable, so the requirements don't restrict them in advance. |
| C08 | Every requirement and acceptance criterion must be verifiable. Terms like "fast" or "easy" are only allowed with a measurable criterion. | Makes it possible to check objectively whether a feature is correctly implemented. |

## 4. Verification

```gherkin
Scenario: Checking a requirement or user story against the constraints
  Given a new or changed requirement or user story
  When it is reviewed before being accepted into the backlog
  Then it does not introduce sprints, reports, external integrations or configurable behaviour (C01–C04)
  And it is written in English and requires English interface text (C05, C06)
  And it does not name any technology, database, architecture or platform (C07)
  And it can be checked objectively (C08)
```
