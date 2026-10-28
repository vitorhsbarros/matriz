# US11 — Create a team

| Priority | Order | Requirements | Depends on |
|---|---|---|---|
| Must (MVP) | 11 | FR08 | US02 |

## User story

**As a** user,
**I want** to create a team,
**so that** I can assign tasks to a group of people instead of just one.

## Rules

| Field | Rules |
|---|---|
| Name | Required, between 1 and 50 characters. Unique across all teams, case-insensitively (D06). |

- Whoever creates the team becomes its manager and a member (D07).
- Every user sees the list of teams, with each team's name and members (D05).

## Acceptance criteria

Shared context for all scenarios: Ana is logged in.

### Main scenario

```gherkin
Scenario: AC01 — Create a team
  Given there is no team called "Design"
  When Ana creates the team "Design"
  Then the team "Design" is created
  And Ana becomes the team's manager and a member
  And the team "Design" appears in every user's team list
```

### Exceptional situations

```gherkin
Scenario Outline: AC02 — Missing name
  When Ana creates a team with the name <name>
  Then the message "Team name is required." is shown
  And no team is created

  Examples:
    | name  |
    | ""    |
    | "   " |

Scenario: AC03 — Name already in use
  Given the team "Design" already exists
  When Ana creates a team with the name "Design"
  Then the message "A team with this name already exists." is shown
  And no team is created
```

### Edge cases

```gherkin
Scenario: AC04 — Name already in use with different casing and spaces
  Given the team "Design" already exists
  When Ana creates a team with the name "  DESIGN  "
  Then the message "A team with this name already exists." is shown

Scenario Outline: AC05 — Name limits
  When Ana creates a team with a name of <n> characters
  Then the result is <result>

  Examples:
    | n  | result                                                  |
    | 1  | the team is created                                     |
    | 50 | the team is created                                     |
    | 51 | the message "Team name must be at most 50 characters."  |
```
