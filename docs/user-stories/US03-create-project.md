# US03 — Create a project

| Priority | Order | Requirements | Depends on |
|---|---|---|---|
| Must (MVP) | 3 | FR03 | US02 |

## User story

**As a** user,
**I want** to create a project with a name and a description,
**so that** I can gather a piece of work's tasks in one place.

## Rules

| Field | Rules |
|---|---|
| Name | Required, between 1 and 100 characters. Can repeat (D06). |
| Description | Optional, up to 2000 characters. |

Whoever creates the project becomes its manager and a member (D07).

## Acceptance criteria

### Main scenario

```gherkin
Scenario: AC01 — Create a project with name and description
  Given Ana is logged in
  When she creates a project with the name "Website" and the description "New company website"
  Then the project "Website" is created with the description "New company website"
  And Ana becomes the project's manager and a member
  And the project has no tasks
  And the project "Website" appears in Ana's project list

Scenario: AC02 — Create a project without a description
  Given Ana is logged in
  When she creates a project with the name "Website" and no description
  Then the project "Website" is created with an empty description
```

### Exceptional situations

```gherkin
Scenario Outline: AC03 — Missing name
  When Ana creates a project with the name <name>
  Then the message "Project name is required." is shown
  And no project is created

  Examples:
    | name  |
    | ""    |
    | "   " |
```

### Edge cases

```gherkin
Scenario Outline: AC04 — Name limits
  When Ana creates a project with a name of <n> characters
  Then the result is <result>

  Examples:
    | n   | result                                                      |
    | 1   | the project is created                                      |
    | 100 | the project is created                                      |
    | 101 | the message "Project name must be at most 100 characters."  |

Scenario Outline: AC05 — Description limits
  When Ana creates a project with a description of <n> characters
  Then the result is <result>

  Examples:
    | n    | result                                                      |
    | 2000 | the project is created                                      |
    | 2001 | the message "Description must be at most 2000 characters."  |

Scenario: AC06 — Repeated names are allowed
  Given a project called "Website" already exists
  When Ana creates another project with the name "Website"
  Then a second project is created, separate from the first
  And the two projects have independent tasks and members
```
