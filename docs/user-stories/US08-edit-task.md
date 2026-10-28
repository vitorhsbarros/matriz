# US08 — Edit a task

| Priority | Order | Requirements | Depends on |
|---|---|---|---|
| Must (MVP) | 8 | FR11 | US05 |

## User story

**As a** project member,
**I want** to edit a task's title, description and due date,
**so that** I can keep the task's information correct without recreating it.

## Rules

- Field validations are the same as in [US05](US05-create-task.md).
- Editing doesn't change the task's status or assignee.
- The description and due date can be cleared.
- Any project member can edit any task in the project (D08).

## Acceptance criteria

Shared context for all scenarios: Ana is a member of the project "Website", today is 2026-11-02 and the project has this task:

| title | description | due date | status | assignee |
|---|---|---|---|---|
| Test the site | Test on mobile | 2026-11-20 | In progress | Rui Costa |

### Main scenario

```gherkin
Scenario: AC01 — Edit every field
  When Ana edits the task "Test the site" to:
    | title       | Test the site in production |
    | description | Test on mobile and tablet   |
    | due date    | 2026-11-25                  |
  Then the task has that data
  And the status stays "In progress"
  And the assignee stays "Rui Costa"

Scenario: AC02 — Clear the description and due date
  When Ana clears the description and due date of the task "Test the site"
  Then the task has no description and no due date
```

### Exceptional situations

```gherkin
Scenario: AC03 — Title cleared
  When Ana edits the title of the task "Test the site" to ""
  Then the message "Task title is required." is shown
  And the task keeps all its previous data

Scenario: AC04 — New due date in the past
  When Ana changes the due date of the task "Test the site" to 2026-11-01
  Then the message "Due date cannot be in the past." is shown
  And the due date stays 2026-11-20

Scenario: AC05 — An error in one field doesn't save the others
  When Ana changes the description to "New description" and the title to ""
  Then the message "Task title is required." is shown
  And the description stays "Test on mobile"

Scenario: AC06 — User who is not a project member
  Given Marta is not a member of the project "Website"
  When Marta tries to edit the task "Test the site"
  Then the message "Project not found." is shown
  And the task keeps all its previous data
```

### Edge cases

```gherkin
Scenario: AC07 — Past due date that isn't changed (D12)
  Given the task "Test the site" has the due date 2026-10-30, which has passed
  When Ana edits only the task title to "Test the final site"
  Then the change is saved
  And the due date stays 2026-10-30

Scenario: AC08 — Save without changes
  When Ana saves the task "Test the site" without changing any field
  Then the task keeps all its data
  And no error message is shown

Scenario: AC09 — Two simultaneous edits (D11)
  Given Ana and Rui both have the task "Test the site" open for editing
  When Ana saves the title "Title A"
  And then Rui saves the title "Title B"
  Then the task's final title is "Title B"
```
