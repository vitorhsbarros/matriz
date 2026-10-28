# US04 — View my projects

| Priority | Order | Requirements | Depends on |
|---|---|---|---|
| Must (MVP) | 4 | FR07, NFR03 | US03 |

## User story

**As a** user,
**I want** to see the list of projects I am a member of,
**so that** I can quickly get to the work in each one.

## Rules

- For each project, the list shows the name and the user's role: "Manager" or "Member".
- The list is sorted alphabetically by name, case-insensitively.

## Acceptance criteria

### Main scenario

```gherkin
Scenario: AC01 — See the projects I am a member of
  Given Ana is the manager of the project "Website"
  And Ana is a member of the project "App"
  And there is a project "Shop" that Ana is not a member of
  When Ana opens the project list
  Then the list shows, in this order:
    | project | role    |
    | App     | Member  |
    | Website | Manager |
  And the project "Shop" is not shown

Scenario: AC02 — Open a project from the list
  Given the project "Website" appears in Ana's project list
  When Ana opens the project "Website"
  Then the task list of the project "Website" is shown (US06)
```

### Exceptional situations

```gherkin
Scenario: AC03 — Not a member of any project
  Given Ana is not a member of any project
  When she opens the project list
  Then the message "You are not a member of any project yet." is shown

Scenario: AC04 — Removed from a project
  Given Ana was a member of the project "App"
  And the manager removed Ana from the project "App" (US09)
  When Ana opens the project list
  Then the project "App" is not shown
  And if Ana tries to open the project "App" directly, the message "Project not found." is shown
```

### Edge cases

```gherkin
Scenario: AC05 — Case-insensitive sorting
  Given Ana is a member of the projects "beta", "Alpha" and "Gamma"
  When she opens the project list
  Then the order shown is "Alpha", "beta", "Gamma"

Scenario: AC06 — Projects with the same name
  Given Ana is a member of two different projects called "Website"
  When she opens the project list
  Then two "Website" entries are shown
  And each one opens its own project
```
