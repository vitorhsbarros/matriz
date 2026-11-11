# Matriz — Constraints, assumptions and pending decisions

This document keeps everything that limits or conditions the solution, apart from the confirmed requirements in [requirements.md](requirements.md):

- **known constraints:** fixed decisions the solution must respect;
- **assumptions:** what is taken as true without being confirmed;
- **security and privacy** rules;
- **pending decisions:** technical decisions not yet made;
- **scope limitations:** what Matriz will never do.

Every requirement in [requirements.md](requirements.md) and every user story in [user-stories.md](user-stories.md) must comply with the constraints below.

## 1. Known constraints

| ID | Constraint | Justification |
|---|---|---|
| C05 | All of the application's interface and messages are in English. | Chosen language for the application. Previously NFR01. |
| C06 | All project documentation (requirements, constraints, user stories, prompts) is written in English. | Keeps the documentation consistent with the application and readable by anyone working on the project. |
| C07 | Requirements and user stories are technology-independent: they don't prescribe a technology stack, database, architecture or platform. | Technical decisions are made later, once the requirements are stable, so the requirements don't restrict them in advance. Architecture decisions are recorded as ADRs in [architecture.md](architecture.md). |
| C08 | Every requirement and acceptance criterion must be verifiable. Terms like "fast" or "easy" are only allowed with a measurable criterion. | Makes it possible to check objectively whether a feature is correctly implemented. |

The scope constraints C01–C04 are in section 5.

## 2. Assumptions

These are taken as true but have not been confirmed by a user or a stakeholder. If one turns out to be wrong, the requirements or the architecture may need to change.

### About the product

| ID | Assumption | Impact if wrong |
|---|---|---|
| ASM01 | **Accounts from the seed:** in the MVP, accounts are the demo accounts created by the seed (FR21). Public sign-up is an optional extension (FR01). There is no system administrator role. | An administrator role would need new requirements, user stories and permissions. If sign-up is added, anyone can create an account (see PRI04). |
| ASM02 | **One manager per project and per team:** the manager is whoever creates the project or team, and the role can't be transferred (see F04). | Transferring the role would need a new requirement and a stored role (database.md, A3). |
| ASM03 | **Fixed statuses:** a task always has one of three statuses ("To do", "In progress", "Done"). Any transition between them is allowed. | Restricted transitions would change US07 and the board. Configurable statuses are excluded by C04. |
| ASM04 | **Several assignees per task:** a task can have zero or more assignees, each one a person or a team, and people and teams can be mixed (FR14). | Limiting it to one assignee, or to people only, would change the data model, the API and US10/US13. |
| ASM05 | **Teams independent of projects:** a team exists outside projects and can work on several. Later changes to a team don't undo its existing assignments (D09). | A team owned by a single project would simplify the rules but limit reuse. |
| ASM06 | **Removing members:** when someone leaves a project, they are removed from the assignees of that project's tasks; the other assignees stay (D10). | Keeping them assigned would show tasks owned by someone without access. |
| ASM12 | **Target users:** the client's teams are small and don't need sprints, reports or configurable workflows. | If wrong, the scope limitations C01–C04 must be revisited. |
| ASM13 | **Team assignment:** the client's teams share tasks, so a task can be assigned to a whole team. | If wrong, FR08, FR09 and US11–US13 can move out of the MVP. |

### About the context

| ID | Assumption | Impact if wrong |
|---|---|---|
| ASM07 | **Team:** a single developer, with experience in Java and Spring Boot. | More developers would make a stricter module split more valuable (ADR-0001). |
| ASM08 | **Timeline:** about 3 weeks for the MVP (23 user stories). **Risk:** the scope reconciliation made the MVP larger (comments, priority, several assignees, search, filters and the dashboard were added, and teams were kept), so 3 weeks is tight. | Less time would favour a plain monolith, or moving teams or comment editing out of the MVP; more time could afford more structure (ADR-0001). |
| ASM09 | **Scale:** about 50 users, equivalent to 2 or 3 pilot teams. | Thousands of active users would require revisiting the scalability options in ADR-0001. |
| ASM10 | **Deployment:** the MVP runs on one machine and is demonstrated on a local server. | A remote demo needs HTTPS for the session cookie (ADR-0002). |
| ASM11 | **Client:** the only client is a browser application that calls the JSON API in [openapi.yaml](../openapi.yaml), served from the same site as the API. | Other clients (e.g. mobile) would need a different authentication mechanism (ADR-0002). |

Further assumptions specific to one artefact are recorded where they apply: the data model (database.md, A1–A5) and the API contract (api-contract.md, AS01–AS15).

## 3. Security and privacy

### Requirements

| ID | Rule | Source |
|---|---|---|
| SEC01 | Only logged-in users can access projects, teams and tasks. | NFR02, DoD02 |
| SEC02 | Users can only see and change projects and tasks they are members of. Manager-only actions are refused to anyone else. | NFR03, DoD03 |
| SEC03 | A project that the user can't access gets the same "Project not found." response as a project that doesn't exist, so users can't find out which projects exist. | DoD03 |
| SEC04 | A failed log-in gets the same message for a wrong password and an unknown email, so it doesn't reveal which emails have an account. | US02 AC09 |
| SEC05 | Passwords are never stored or shown in readable form, and are never returned by the API. | NFR04, US01 AC10, US19 AC03 |
| SEC06 | Sessions are kept on the server and identified by a session cookie that page scripts can't read (`HttpOnly`, `Secure`, `SameSite=Lax`). Log-out ends the session immediately. | ADR-0002, US02 AC02 |
| SEC07 | Every request that changes data while logged in must carry the session's verification (CSRF) token. | ADR-0002, DoD06 |
| SEC08 | Outside the local machine, the application is only served over HTTPS. | ADR-0002 |
| SEC09 | Adding a member by email doesn't reveal whether the email has an account: an email without an account gets the generic message "This user could not be added." | US09 AC04, US12 AC03 |
| SEC10 | The demo accounts' passwords are never written in the repository, the documentation or the logs. They come from configuration kept outside the repository. | FR21, US19, AGENTS.md §7 |
| SEC11 | Only the author of a comment can edit or delete it. | NFR03, US22 |

### Privacy

| ID | Rule | Source |
|---|---|---|
| PRI01 | The only personal data kept about a person is their name, email and password, plus the comments they write. | FR01, FR21, FR23 |
| PRI02 | No data is sent to external systems. | C03 |
| PRI03 | A person's email is visible only to the members of projects they belong to. | US09 |
| PRI04 | Team names and the names of team members are visible to every user, so tasks can be assigned to teams. Emails of team members aren't shown. Risk: if public sign-up (FR01) is added, anyone who creates an account can see these names (PD11). | D05 |
| PRI05 | The demo accounts and any seed data use made-up names and emails, never real people's personal data. | FR21, US19 |

## 4. Pending decisions

Technical decisions that are still open. They must be decided, by the user, before or during the implementation they affect.

| ID | Decision | Where it comes from |
|---|---|---|
| PD01 | **Technology stack:** languages, frameworks, database product and how the browser application is built. | C07, architecture.md |
| PD02 | **Session expiry:** how long a session lasts. Until decided, sessions last until log-out and abandoned sessions stay in the database. | ADR-0002, review-01 S01 |
| PD03 | **Tool for checking module boundaries** automatically. | ADR-0001, 04-solution-architecture open point 3 |
| PD04 | **References between modules' data:** protected by database rules (foreign keys) or only by identifiers. | database.md OP1 |
| PD05 | **Assigning and removing a member at the same time** (US10 AC05): the transaction isolation or ordering that prevents an assignment from surviving a removal. | 04-solution-architecture, risks |
| PD06 | **How passwords are protected:** a one-way, salted and deliberately slow hashing method, never reversible encryption (NFR04). The specific algorithm is chosen with the stack. | NFR04 |
| PD07 | **Deleting an account and its personal data.** Not in the MVP. Needed before real users' data is kept long-term. | PRI01 |
| PD08 | **Scalability as a formal requirement.** | 04-solution-architecture open point 4 |
| PD09 | **Review-01 item without a decision:** F05 (membership role wording in 04-solution-architecture). Its S02 (US06 title) was resolved by specification-review SR14. | [review-01](reviews/review-01.md) |
| PD10 | **Protection against repeated log-in attempts** (e.g. a temporary block after several failures for the same email). Not in the MVP requirements. | specification-review SR09 |
| PD11 | **Who can see teams:** every user (D05), or only users who share a project with the team's members. | specification-review SR10 |
| PD12 | **Demo accounts:** how many there are, and how the seed is run and gets the passwords from configuration. Decided with the stack (PD01). | FR21, [scope reconciliation](reviews/scope-reconciliation.md) SC03 |

## 5. Scope limitations

The application is deliberately simple: a stripped-down alternative to Jira. These features are excluded from every version, not just postponed.

| ID | Constraint | Justification |
|---|---|---|
| C01 | The application does not support sprints or any other form of iteration planning. | Sprints are one of the main sources of complexity in tools like Jira and are not needed to organise tasks within a project. |
| C02 | The application does not produce reports, charts or statistics. Plain counts of tasks per status, like those on the board and the home dashboard (FR07, FR15), are allowed. | Progress is followed through task statuses on the board (FR13, FR15) and their counts. Reports would add a feature area without serving the core idea. |
| C03 | The application does not integrate with external systems (e.g. email, calendars, chat tools, third-party login, other project management tools). | Integrations add dependencies and configuration that go against keeping the application simple. Password recovery (F08) must be revisited against this constraint before it is built. |
| C04 | Features that make the application configurable (e.g. custom statuses, custom fields, custom workflows) are excluded. | Configurability is what makes tools like Jira complex. Fixed behaviour keeps the application easy to learn. This is why F06 and F07 are rated Won't. |

Features postponed rather than excluded (the Should requirements and the future features F01–F07) are listed in [requirements.md, section 7](requirements.md#7-out-of-the-initial-scope).

## 6. Verification

```gherkin
Scenario: Checking a requirement or user story against the constraints
  Given a new or changed requirement or user story
  When it is reviewed before being accepted into the backlog
  Then it does not introduce sprints, reports, external integrations or configurable behaviour (C01–C04)
  And it is written in English and requires English interface text (C05, C06)
  And it does not name any technology, database, architecture or platform (C07)
  And it can be checked objectively (C08)
  And it respects the security and privacy rules (SEC01–SEC11, PRI01–PRI05)
```
