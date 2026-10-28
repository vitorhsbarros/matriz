# US13 — Assign a task to a team

| Priority | Order | Requirements | Depends on |
|---|---|---|---|
| Must (MVP) | 13 | FR14 | US10, US12 |

## User story

**As a** project member,
**I want** to assign a task to a team,
**so that** a group of people is jointly responsible for it.

## Rules

- A task has at most one assignee: a person ([US10](US10-assign-task-to-person.md)) or a team.
- A task can only be assigned to a team whose members are all project members. This rule is only checked at the moment of assignment (D09).
- Assigning replaces the previous assignee, whether a person or another team.
- An assignment to a team is undone the same way as an assignment to a person (US10, AC03).

## Acceptance criteria

Shared context for all scenarios: Ana, Rui and Sara are members of the project "Website", and the team "Design" has Rui and Sara as members.

### Main scenario

```gherkin
Scenario: AC01 — Assign to a team
  Given the task "Create icons" has no assignee
  When Ana assigns the task "Create icons" to the team "Design"
  Then the task's assignee becomes the team "Design"
  And the task list shows "Design (team)" as the assignee

Scenario: AC02 — Replace a person with a team
  Given the task "Create icons" is assigned to Rui
  When Ana assigns the task to the team "Design"
  Then the task's assignee becomes the team "Design"
  And Rui is no longer individually responsible for the task

Scenario: AC03 — Replace a team with a person
  Given the task "Create icons" is assigned to the team "Design"
  When Ana assigns the task to Sara
  Then the task's assignee becomes Sara
  And the team "Design" is no longer responsible for the task
```

### Exceptional situations

```gherkin
Scenario: AC04 — Team with members outside the project
  Given the team "Marketing" has Rui and Marta as members
  And Marta is not a member of the project "Website"
  When Ana tries to assign the task "Create icons" to the team "Marketing"
  Then the message "All team members must belong to the project." is shown
  And the task's assignee doesn't change

Scenario: AC05 — Non-existent team
  When Ana tries to assign the task "Create icons" to a team that doesn't exist
  Then the message "Team not found." is shown
  And the task's assignee doesn't change
```

### Edge cases

```gherkin
Scenario: AC06 — Team with no members besides the manager
  Given the team "QA" has only Ana as a member
  When Ana assigns the task "Create icons" to the team "QA"
  Then the task's assignee becomes the team "QA"

Scenario: AC07 — Team members change after the assignment (D09)
  Given the task "Create icons" is assigned to the team "Design"
  When the manager of the team "Design" adds Marta, who is not a member of the project "Website"
  Then the task "Create icons" stays assigned to the team "Design"

Scenario: AC08 — Team member removed from the project (D10)
  Given the task "Create icons" is assigned to the team "Design"
  When the project manager removes Rui from the project "Website"
  Then the task "Create icons" stays assigned to the team "Design"
```
