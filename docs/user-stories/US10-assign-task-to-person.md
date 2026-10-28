# US10 — Assign a task to a person

| Priority | Order | Requirements | Depends on |
|---|---|---|---|
| Must (MVP) | 10 | FR14 | US05, US09 |

## User story

**As a** project member,
**I want** to assign a task to a project member, or undo that assignment,
**so that** it's clear who is responsible for it.

## Rules

- A task has at most one assignee: a person or a team ([US13](US13-assign-task-to-team.md)).
- A task can only be assigned to a person who is a project member.
- Assigning a task that already has an assignee replaces the previous assignee.
- Any project member can assign any task in the project (D08).

## Acceptance criteria

Shared context for all scenarios: Ana and Rui are members of the project "Website", which has the task "Test the site" with no assignee.

### Main scenario

```gherkin
Scenario: AC01 — Assign to a member
  When Ana assigns the task "Test the site" to Rui
  Then the task's assignee becomes Rui
  And the task list shows "Rui Costa" as the assignee

Scenario: AC02 — Replace the assignee
  Given the task "Test the site" is assigned to Rui
  When Ana assigns the task to herself
  Then the task's assignee becomes Ana
  And Rui is no longer responsible for the task

Scenario: AC03 — Undo the assignment
  Given the task "Test the site" is assigned to Rui
  When Ana undoes the task's assignment
  Then the task becomes unassigned ("Unassigned")
```

### Exceptional situations

```gherkin
Scenario: AC04 — Person who is not a project member
  Given Marta is not a member of the project "Website"
  When Ana tries to assign the task "Test the site" to Marta
  Then the message "The assignee must be a member of the project." is shown
  And the task stays unassigned

Scenario: AC05 — Member removed in the meantime
  Given Ana opened the task "Test the site" to assign it to Rui
  And in the meantime the manager removed Rui from the project
  When Ana confirms the assignment to Rui
  Then the message "The assignee must be a member of the project." is shown
  And the task stays unassigned

Scenario: AC06 — User who is not a project member
  Given Marta is not a member of the project "Website"
  When Marta tries to assign the task "Test the site"
  Then the message "Project not found." is shown
  And the task stays unassigned
```

### Edge cases

```gherkin
Scenario: AC07 — Assign to the current assignee
  Given the task "Test the site" is assigned to Rui
  When Ana assigns the task to Rui
  Then the assignee stays Rui
  And no error message is shown

Scenario: AC08 — Undo the assignment of an unassigned task
  Given the task "Test the site" has no assignee
  When Ana undoes the assignment
  Then the task stays unassigned
  And no error message is shown

Scenario: AC09 — Assigning doesn't change other fields
  When Ana assigns the task "Test the site" to Rui
  Then the task's title, description, due date and status don't change
```
