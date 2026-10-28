# US02 — Log in and log out

| Priority | Order | Requirements | Depends on |
|---|---|---|---|
| Must (MVP) | 2 | FR02, NFR02 | US01 |

## User story

**As a** registered user,
**I want** to log in and log out with my email and password,
**so that** I can access my projects without anyone else doing so on my behalf.

## Acceptance criteria

Shared context for all scenarios: there is an account with the email "ana@example.com" and the password "secure123".

### Main scenario

```gherkin
Scenario: AC01 — Successful log in
  Given the visitor is not logged in
  When they log in with the email "ana@example.com" and the password "secure123"
  Then they are logged in with the account "ana@example.com"
  And the project list is shown (US04)

Scenario: AC02 — Log out
  Given Ana is logged in
  When she logs out
  Then she is no longer logged in
  And when she tries to access a project, the message "Please log in to continue." is shown
```

### Exceptional situations

```gherkin
Scenario: AC03 — Wrong password
  When the visitor logs in with the email "ana@example.com" and the password "wrong123"
  Then the message "Invalid email or password." is shown
  And they are not logged in

Scenario: AC04 — Unregistered email
  When the visitor logs in with the email "rui@example.com" and any password
  Then the message "Invalid email or password." is shown
  And they are not logged in

Scenario Outline: AC05 — Missing fields
  When the visitor logs in with the email <email> and the password <password>
  Then the message "Email and password are required." is shown
  And they are not logged in

  Examples:
    | email             | password    |
    | ""                | "secure123" |
    | "ana@example.com" | ""          |
    | ""                | ""          |

Scenario: AC06 — A visitor tries to access a protected feature (NFR02)
  Given the visitor is not logged in
  When they try to access the project list, a project, a task or a team
  Then the message "Please log in to continue." is shown
  And they are asked to log in
```

### Edge cases

```gherkin
Scenario: AC07 — Email with different casing
  When the visitor logs in with the email "ANA@example.com" and the password "secure123"
  Then they are logged in with the account "ana@example.com"

Scenario: AC08 — The password is case-sensitive
  When the visitor logs in with the email "ana@example.com" and the password "SECURE123"
  Then the message "Invalid email or password." is shown

Scenario: AC09 — The message doesn't reveal whether the email exists
  Then the message shown in AC03 and in AC04 is exactly the same
```
