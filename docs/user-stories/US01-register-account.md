# US01 — Register an account

| Priority | Order | Requirements | Depends on |
|---|---|---|---|
| Must (MVP) | 1 | FR01, NFR04 | — |

## User story

**As a** visitor,
**I want** to create an account with my name, email and password,
**so that** I can take part in Matriz projects.

## Rules

| Field | Rules |
|---|---|
| Name | Required, between 1 and 100 characters. |
| Email | Required, in the format `text@domain.extension`, at most 254 characters. Must be unique (case-insensitive, D03). |
| Password | Required, between 8 and 128 characters. Spaces are not trimmed. |

## Acceptance criteria

### Main scenario

```gherkin
Scenario: AC01 — Successful sign-up
  Given there is no account with the email "ana@example.com"
  When the visitor signs up with the name "Ana Silva", the email "ana@example.com" and the password "secure123"
  Then an account is created with the name "Ana Silva" and the email "ana@example.com"
  And the visitor is logged in with that account
  And the project list is shown (US04)
```

### Exceptional situations

```gherkin
Scenario: AC02 — Email already registered
  Given an account with the email "ana@example.com" already exists
  When the visitor signs up with the email "ana@example.com"
  Then the message "This email is already in use." is shown
  And no account is created

Scenario: AC03 — Email already registered with different casing
  Given an account with the email "ana@example.com" already exists
  When the visitor signs up with the email "Ana@Example.COM"
  Then the message "This email is already in use." is shown
  And no account is created

Scenario Outline: AC04 — Missing required fields
  When the visitor signs up leaving the <field> field with the value <value>
  Then the message <message> is shown
  And no account is created

  Examples:
    | field    | value | message                 |
    | name     | ""    | "Name is required."     |
    | name     | "   " | "Name is required."     |
    | email    | ""    | "Email is required."    |
    | password | ""    | "Password is required." |

Scenario Outline: AC05 — Invalid email format
  When the visitor signs up with the email <email>
  Then the message "Enter a valid email address." is shown
  And no account is created

  Examples:
    | email             |
    | "ana"             |
    | "ana@"            |
    | "ana.example.com" |
    | "@example.com"    |
    | "ana@example"     |
```

### Edge cases

```gherkin
Scenario Outline: AC06 — Name limits
  When the visitor signs up with a name of <n> characters
  Then the result is <result>

  Examples:
    | n   | result                                              |
    | 1   | the account is created                              |
    | 100 | the account is created                              |
    | 101 | the message "Name must be at most 100 characters."  |

Scenario Outline: AC07 — Password limits
  When the visitor signs up with a password of <n> characters
  Then the result is <result>

  Examples:
    | n   | result                                                 |
    | 7   | the message "Password must be at least 8 characters."  |
    | 8   | the account is created                                 |
    | 128 | the account is created                                 |
    | 129 | the message "Password must be at most 128 characters." |

Scenario: AC08 — Leading and trailing spaces in name and email
  When the visitor signs up with the name "  Ana Silva  " and the email "  ana@example.com  "
  Then the account is created with the name "Ana Silva" and the email "ana@example.com"
```

### Password security (NFR04)

```gherkin
Scenario: AC09 — Password hidden while typing
  When the visitor types the password in the sign-up form
  Then each character is shown as a mask symbol, not as the typed character

Scenario: AC10 — Password not stored in readable text
  Given the visitor signed up with the password "secure123"
  When the stored data for that account is inspected
  Then the text "secure123" does not appear in any of the stored data
```
