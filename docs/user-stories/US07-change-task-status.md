# US07 — Change a task's status

| Priority | Order | Requirements | Depends on |
|---|---|---|---|
| Must (MVP) | 7 | FR13, NFR07 | US05 |

## User story

**As a** project member,
**I want** to change a task's status,
**so that** everyone knows where the work stands.

## Rules

- The possible statuses are "To do", "In progress" and "Done".
- Any transition between statuses is allowed, including going back.
- Any project member can change the status of any task in the project, even if they aren't the assignee (D08).
- Changing the status doesn't change any other field of the task.

## Acceptance criteria

Shared context for all scenarios: Ana and Rui are members of the project "Website", which has the task "Test the site" in the "To do" status.

### Main scenario

```gherkin
Scenario: AC01 — Change a task's status
  When Ana changes the status of the task "Test the site" to "In progress"
  Then the task status becomes "In progress"
  And the title, description, due date and assignee don't change

Scenario Outline: AC02 — Every transition is allowed
  Given the task "Test the site" is in the status <from>
  When Ana changes the status to <to>
  Then the task status becomes <to>

  Examples:
    | from        | to          |
    | To do       | In progress |
    | To do       | Done        |
    | In progress | To do       |
    | In progress | Done        |
    | Done        | To do       |
    | Done        | In progress |

Scenario: AC03 — The change is visible to other members (NFR07)
  Given Ana changed the status of the task "Test the site" to "Done"
  When Rui opens the project "Website" after that change
  Then Rui sees the task "Test the site" with the status "Done"
```

### Exceptional situations

```gherkin
Scenario: AC04 — Invalid status
  When the status of the task "Test the site" is requested to become "Blocked"
  Then the message "Invalid status." is shown
  And the task status stays "To do"

Scenario: AC05 — User who is not a project member
  Given Marta is not a member of the project "Website"
  When Marta tries to change the status of the task "Test the site"
  Then the message "Project not found." is shown
  And the task status stays "To do"
```

### Edge cases

```gherkin
Scenario: AC06 — Choose the current status
  When Ana changes the status of the task "Test the site" to "To do"
  Then the task status stays "To do"
  And no error message is shown

Scenario: AC07 — Member who is not the assignee
  Given the task "Test the site" is assigned to Rui
  When Ana changes the task status to "Done"
  Then the task status becomes "Done"

Scenario: AC08 — Two simultaneous changes (D11)
  Given Ana and Rui both have the task "Test the site" open
  When Ana changes the status to "In progress"
  And then Rui changes the status to "Done"
  Then the task's final status is "Done"
```
