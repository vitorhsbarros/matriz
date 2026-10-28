# US12 — Manage a team's members

| Priority | Order | Requirements | Depends on |
|---|---|---|---|
| Must (MVP) | 12 | FR09 | US11 |

## User story

**As a** team manager,
**I want** to add and remove people from the team,
**so that** the team reflects who works together.

## Rules

- People are added by their account email (D04).
- Only the team manager can add and remove members.
- The manager can't be removed (D07).
- Changing a team's members doesn't undo task assignments already made to that team (D09).

## Acceptance criteria

Shared context for all scenarios: Ana is the manager of the team "Design" and Rui has an account with the email "rui@example.com".

### Main scenario

```gherkin
Scenario: AC01 — Add a member
  Given Rui is not a member of the team "Design"
  When Ana adds the email "rui@example.com" to the team "Design"
  Then Rui becomes a member of the team "Design"
  And the team list shows Rui as a member of the team "Design"

Scenario: AC02 — Remove a member
  Given Rui is a member of the team "Design"
  When Ana removes Rui from the team "Design"
  Then Rui is no longer a member of the team "Design"
```

### Exceptional situations

```gherkin
Scenario: AC03 — Email without an account
  When Ana adds the email "marta@example.com", which has no account
  Then the message "No user found with this email." is shown
  And the team's members don't change

Scenario: AC04 — Person who is already a member
  Given Rui is already a member of the team "Design"
  When Ana adds the email "rui@example.com"
  Then the message "This user is already a member of the team." is shown
  And the team's members don't change

Scenario: AC05 — Try to remove the manager
  When Ana tries to remove herself from the team "Design"
  Then the message "The team manager cannot be removed." is shown
  And Ana remains the team's manager

Scenario: AC06 — A member who isn't the manager tries to manage members
  Given Rui is a member, but not the manager, of the team "Design"
  When Rui tries to add or remove a member
  Then the message "Only the team manager can manage members." is shown
  And the team's members don't change
```

### Edge cases

```gherkin
Scenario: AC07 — Email with different casing (D03)
  When Ana adds the email "RUI@Example.com"
  Then Rui becomes a member of the team "Design"

Scenario: AC08 — Changing the team doesn't undo existing assignments (D09)
  Given the task "Create icons" in the project "Website" is assigned to the team "Design"
  And Marta is not a member of the project "Website"
  When Ana adds Marta to the team "Design"
  Then the task "Create icons" stays assigned to the team "Design"
  And Marta still has no access to the project "Website"
```
