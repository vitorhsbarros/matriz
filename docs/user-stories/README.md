# Matriz — Initial backlog (MVP)

This backlog turns the Must requirements in [requirements-v2.md](../requirements-v2.md) into verifiable user stories (US). Each US has its own file in this folder. Every US must also comply with the constraints in [constraints.md](../constraints.md).

## 1. Backlog

Every MVP user story has **Must** priority. The **Order** column gives the recommended implementation sequence, based on the dependencies between stories and the value each one delivers.

| Order | US | Title | Requirements | Depends on |
|---|---|---|---|---|
| 1 | [US01](US01-register-account.md) | Register an account | FR01, NFR04 | — |
| 2 | [US02](US02-log-in-and-out.md) | Log in and log out | FR02, NFR02 | US01 |
| 3 | [US03](US03-create-project.md) | Create a project | FR03 | US02 |
| 4 | [US04](US04-view-my-projects.md) | View my projects | FR07, NFR03 | US03 |
| 5 | [US05](US05-create-task.md) | Create a task | FR10 | US03 |
| 6 | [US06](US06-list-project-tasks.md) | List a project's tasks | FR15 | US05 |
| 7 | [US07](US07-change-task-status.md) | Change a task's status | FR13, NFR07 | US05 |
| 8 | [US08](US08-edit-task.md) | Edit a task | FR11 | US05 |
| 9 | [US09](US09-manage-project-members.md) | Manage a project's members | FR06, NFR03 | US03 |
| 10 | [US10](US10-assign-task-to-person.md) | Assign a task to a person | FR14 | US05, US09 |
| 11 | [US11](US11-create-team.md) | Create a team | FR08 | US02 |
| 12 | [US12](US12-manage-team-members.md) | Manage a team's members | FR09 | US11 |
| 13 | [US13](US13-assign-task-to-team.md) | Assign a task to a team | FR14 | US10, US12 |

FR14 was split into two stories (US10 and US13). Assigning tasks to people can be delivered before teams exist, which makes the product usable sooner.

## 2. Cross-cutting criteria (Definition of Done)

These criteria apply to **every** US and are not repeated in each file. A US is only done when its own acceptance criteria and the criteria below are met.

### DoD01 — Interface in English (C05)

```gherkin
Scenario: All displayed text is in English
  Given any screen or message of the feature
  When the user sees it
  Then all text, buttons, labels and messages are in English
```

### DoD02 — Access only when logged in (NFR02)

```gherkin
Scenario: A visitor tries to access a protected feature
  Given a visitor who is not logged in
  When they try to access any project, team or task
  Then access is refused
  And the message "Please log in to continue." is shown
  And they are asked to log in
```

### DoD03 — Access only to projects the user is a member of (NFR03)

```gherkin
Scenario: A user tries to access a project they are not a member of
  Given the user "Ana" is not a member of the project "Website"
  When she tries to view or change the project "Website" or any of its tasks
  Then access is refused
  And the message "Project not found." is shown
  And no project data is shown or changed
```

The message is the same as for a project that doesn't exist. This way, users can't find out which projects exist.

### DoD04 — Exact messages

Quoted messages in the acceptance criteria are the exact text to show. When an action is refused by a validation, no data is created or changed.

## 3. Decisions made to remove ambiguities

The requirements left some points open. These decisions apply to every US.

| # | Decision |
|---|---|
| D01 | **Leading and trailing spaces:** text fields are saved without leading or trailing spaces. A required field containing only spaces counts as empty. |
| D02 | **Length limits:** limits count characters after applying D01. |
| D03 | **Emails:** compared case-insensitively ("Ana@Example.com" and "ana@example.com" are the same email). |
| D04 | **Adding people:** people are added to projects and teams by their account email. |
| D05 | **Teams visible to everyone:** every user sees the list of teams, with each team's name and members, so they can assign tasks to them. |
| D06 | **Names:** project names can repeat, but team names are unique, because teams are chosen by name when assigning tasks. |
| D07 | **Managers are also members:** whoever creates a project or team becomes its manager and a member. The manager can't be removed. |
| D08 | **Any member can change tasks:** any project member can edit, change the status of and assign any task in that project, not just the assignee. |
| D09 | **Team assignment is only checked when assigning:** the rule "all team members belong to the project" is only checked when the task is assigned. Later changes to the team don't undo existing assignments. |
| D10 | **Removing a project member:** tasks assigned to that person in that project become unassigned. Tasks assigned to teams are not affected. |
| D11 | **Simultaneous changes:** if two members change the same task at the same time, the last saved change wins. |
| D12 | **Due dates:** a new or changed due date can't be earlier than the current day. A past due date that isn't changed stays valid. |

## 4. Traceability

Every MVP requirement is covered by at least one US or by a cross-cutting criterion.

| Requirement or constraint | Covered by |
|---|---|
| FR01 | US01 |
| FR02 | US02 |
| FR03 | US03 |
| FR06 | US09 |
| FR07 | US04 |
| FR08 | US11 |
| FR09 | US12 |
| FR10 | US05 |
| FR11 | US08 |
| FR13 | US07 |
| FR14 | US10, US13 |
| FR15 | US06 |
| NFR02 | US02, DoD02 |
| NFR03 | US04, US09, DoD03 |
| NFR04 | US01 |
| NFR07 | US07 |
| C05 | DoD01 |

The Should requirements (FR04, FR05, FR12, FR16, FR17, FR18, NFR05, NFR06) are not in this backlog and will be turned into user stories after the MVP.
