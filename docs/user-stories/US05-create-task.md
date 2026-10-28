# US05 — Create a task

| Priority | Order | Requirements | Depends on |
|---|---|---|---|
| Must (MVP) | 5 | FR10 | US03 |

## User story

**As a** project member,
**I want** to create a task in the project,
**so that** I can record work that needs to be done.

## Rules

| Field | Rules |
|---|---|
| Title | Required, between 1 and 200 characters. |
| Description | Optional, up to 5000 characters. |
| Due date | Optional. Must be a valid date and can't be earlier than the current day (D12). |

A new task always starts in the "To do" status and with no assignee ("Unassigned").

## Acceptance criteria

Shared context for all scenarios: Ana is a member of the project "Website" and today is 2026-11-02.

### Main scenario

```gherkin
Scenario: AC01 — Create a task with every field
  When Ana creates a task in the project "Website" with:
    | title       | Design home page        |
    | description | Follow the style guide  |
    | due date    | 2026-11-20              |
  Then the task "Design home page" is created in the project "Website" with that data
  And the task status is "To do"
  And the task has no assignee ("Unassigned")
  And the task appears in the project's task list (US06)

Scenario: AC02 — Create a task with only a title
  When Ana creates a task in the project "Website" with only the title "Review copy"
  Then the task "Review copy" is created with no description and no due date
```

### Exceptional situations

```gherkin
Scenario Outline: AC03 — Missing title
  When Ana creates a task with the title <title>
  Then the message "Task title is required." is shown
  And no task is created

  Examples:
    | title |
    | ""    |
    | "   " |

Scenario: AC04 — Due date in the past
  When Ana creates a task with the due date 2026-11-01
  Then the message "Due date cannot be in the past." is shown
  And no task is created

Scenario: AC05 — Non-existent due date
  When Ana creates a task with the due date 2027-02-30
  Then the message "Enter a valid date." is shown
  And no task is created

Scenario: AC06 — Create a task in a project I'm not a member of
  Given Rui is not a member of the project "Website"
  When Rui tries to create a task in the project "Website"
  Then the message "Project not found." is shown
  And no task is created
```

### Edge cases

```gherkin
Scenario: AC07 — Due date equal to the current day
  When Ana creates a task with the due date 2026-11-02
  Then the task is created with the due date 2026-11-02

Scenario Outline: AC08 — Title and description limits
  When Ana creates a task with a <field> of <n> characters
  Then the result is <result>

  Examples:
    | field       | n    | result                                                      |
    | title       | 200  | the task is created                                         |
    | title       | 201  | the message "Task title must be at most 200 characters."    |
    | description | 5000 | the task is created                                         |
    | description | 5001 | the message "Description must be at most 5000 characters."  |

Scenario: AC09 — Repeated titles in the same project
  Given the task "Review copy" already exists in the project "Website"
  When Ana creates another task with the title "Review copy" in the same project
  Then a second task is created, separate from the first
```
