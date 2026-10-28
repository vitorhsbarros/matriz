# US06 — List a project's tasks

| Priority | Order | Requirements | Depends on |
|---|---|---|---|
| Must (MVP) | 6 | FR15 | US05 |

## User story

**As a** project member,
**I want** to see all the project's tasks with each one's status and assignee,
**so that** I have an overview of the work and its progress.

## Rules

- Each task shows its title, status, assignee and due date (if any).
- The assignee is shown as:
  - the person's name;
  - the team's name, followed by "(team)";
  - "Unassigned", if there is no assignee.
- Tasks are sorted from newest to oldest by creation date.
- The list shows every task in the project, including done ones.

## Acceptance criteria

Shared context for all scenarios: Ana is a member of the project "Website".

### Main scenario

```gherkin
Scenario: AC01 — See the project's tasks
  Given the project "Website" has these tasks, created in this order:
    | title         | status      | assignee  | due date   |
    | Review copy   | Done        | Rui Costa |            |
    | Create logo   | In progress | Design    | 2026-11-20 |
    | Test the site | To do       |           |            |
  And "Design" is a team
  When Ana opens the project "Website"
  Then the list shows, in this order:
    | title         | status      | assignee      | due date   |
    | Test the site | To do       | Unassigned    |            |
    | Create logo   | In progress | Design (team) | 2026-11-20 |
    | Review copy   | Done        | Rui Costa     |            |
```

### Exceptional situations

```gherkin
Scenario: AC02 — Project with no tasks
  Given the project "Website" has no tasks
  When Ana opens the project "Website"
  Then the message "No tasks yet." is shown

Scenario: AC03 — Project I'm not a member of
  Given Rui is not a member of the project "Website"
  When Rui tries to see the tasks of the project "Website"
  Then the message "Project not found." is shown
  And no task is shown
```

### Edge cases

```gherkin
Scenario: AC04 — Only the open project's tasks are shown
  Given Ana is a member of the projects "Website" and "App"
  And the project "App" has the task "Publish to the store"
  When Ana opens the project "Website"
  Then the task "Publish to the store" is not shown

Scenario: AC05 — The list reflects other members' changes (NFR07)
  Given Rui changed the status of the task "Test the site" to "In progress"
  When Ana opens the project "Website" after that change
  Then the task "Test the site" is shown with the status "In progress"
```
