# US09 — Manage a project's members

| Priority | Order | Requirements | Depends on |
|---|---|---|---|
| Must (MVP) | 9 | FR06, NFR03 | US03 |

## User story

**As a** project manager,
**I want** to add and remove project members,
**so that** the right people can work on it.

## Rules

- Members are added by their account email (D04).
- Every member sees the project's member list, with each member's name, email and role ("Manager" or "Member").
- Only the manager can add and remove members.
- The manager can't be removed (D07).
- When a member is removed, the tasks assigned to that person in the project become unassigned. Tasks assigned to teams don't change (D10).

## Acceptance criteria

Shared context for all scenarios: Ana is the manager of the project "Website" and Rui has an account with the email "rui@example.com".

### Main scenario

```gherkin
Scenario: AC01 — Add a member
  Given Rui is not a member of the project "Website"
  When Ana adds the email "rui@example.com" to the project "Website"
  Then Rui becomes a member of the project, with the role "Member"
  And the project "Website" appears in Rui's project list

Scenario: AC02 — Remove a member
  Given Rui is a member of the project "Website"
  When Ana removes Rui from the project "Website"
  Then Rui is no longer a member of the project
  And the project "Website" no longer appears in Rui's project list

Scenario: AC03 — The removed member's tasks become unassigned (D10)
  Given Rui is a member of the project "Website" and of the team "Design"
  And the task "Create logo" is assigned to Rui
  And the task "Create icons" is assigned to the team "Design"
  When Ana removes Rui from the project "Website"
  Then the task "Create logo" becomes unassigned ("Unassigned")
  And the task "Create icons" stays assigned to the team "Design"
```

### Exceptional situations

```gherkin
Scenario: AC04 — Email without an account
  When Ana adds the email "marta@example.com", which has no account
  Then the message "No user found with this email." is shown
  And the project's members don't change

Scenario: AC05 — Person who is already a member
  Given Rui is already a member of the project "Website"
  When Ana adds the email "rui@example.com"
  Then the message "This user is already a member of the project." is shown
  And the project's members don't change

Scenario: AC06 — Try to remove the manager
  When Ana tries to remove herself from the project "Website"
  Then the message "The project manager cannot be removed." is shown
  And Ana remains the project's manager

Scenario: AC07 — A member who isn't the manager tries to manage members (NFR03)
  Given Rui is a member, but not the manager, of the project "Website"
  When Rui tries to add or remove a member
  Then the message "Only the project manager can manage members." is shown
  And the project's members don't change
```

### Edge cases

```gherkin
Scenario: AC08 — Email with different casing (D03)
  When Ana adds the email "RUI@Example.com"
  Then Rui becomes a member of the project

Scenario: AC09 — Member removed and added again
  Given Rui was removed from the project "Website" and the task "Create logo" became unassigned
  When Ana adds Rui to the project again
  Then Rui is a member of the project again
  And the task "Create logo" stays unassigned
```
