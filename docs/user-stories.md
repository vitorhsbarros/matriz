# Matriz — User stories (MVP)

This is the initial backlog of Matriz. It turns the Must requirements in [requirements.md](requirements.md) into verifiable user stories (US), with priorities, acceptance criteria and exceptional cases. The stories themselves are in section 5. Every US must also comply with the constraints in [constraints.md](constraints.md).

## 1. Backlog

Every MVP user story has **Must** priority. The **Order** column gives the recommended implementation sequence, based on the dependencies between stories and the value each one delivers.

| Order | US | Title | Requirements | Depends on |
|---|---|---|---|---|
| 1 | [US01](#us01--register-an-account) | Register an account | FR01, NFR04 | — |
| 2 | [US02](#us02--log-in-and-log-out) | Log in and log out | FR02, NFR02 | US01 |
| 3 | [US03](#us03--create-a-project) | Create a project | FR03 | US02 |
| 4 | [US04](#us04--view-my-projects) | View my projects | FR07, NFR03 | US03 |
| 5 | [US05](#us05--create-a-task) | Create a task | FR10 | US03 |
| 6 | [US06](#us06--view-a-projects-task-board) | View a project's task board | FR15 | US05 |
| 7 | [US07](#us07--change-a-tasks-status) | Change a task's status | FR13, NFR07 | US05 |
| 8 | [US08](#us08--edit-a-task) | Edit a task | FR11 | US05 |
| 9 | [US09](#us09--manage-a-projects-members) | Manage a project's members | FR06, NFR03 | US03 |
| 10 | [US10](#us10--assign-a-task-to-a-person) | Assign a task to a person | FR14 | US05, US09 |
| 11 | [US11](#us11--create-a-team) | Create a team | FR08 | US02 |
| 12 | [US12](#us12--manage-a-teams-members) | Manage a team's members | FR09 | US11 |
| 13 | [US13](#us13--assign-a-task-to-a-team) | Assign a task to a team | FR14 | US10, US12 |
| 14 | [US14](#us14--edit-a-project) | Edit a project | FR04, NFR03 | US03 |
| 15 | [US15](#us15--delete-a-project) | Delete a project | FR05, NFR03, NFR05 | US03, US05 |
| 16 | [US16](#us16--delete-a-task) | Delete a task | FR12, NFR05 | US05 |
| 17 | [US17](#us17--rename-a-team) | Rename a team | FR19, NFR03 | US11 |
| 18 | [US18](#us18--delete-a-team) | Delete a team | FR20, NFR03, NFR05 | US11, US13 |

FR14 was split into two stories (US10 and US13). Assigning tasks to people can be delivered before teams exist, which makes the product usable sooner.

## 2. Cross-cutting criteria (Definition of Done)

These criteria apply to **every** US and are not repeated in each file. A US is only done when its own acceptance criteria and the criteria below are met.

### DoD01 — Interface in English (C05)

```gherkin
Scenario: All displayed text is in English
  Given any screen or message of the feature
  When the user sees it
  Then all text, buttons, labels and messages are in English
```

### DoD02 — Access only when logged in (NFR02)

```gherkin
Scenario: A visitor tries to access a protected feature
  Given a visitor who is not logged in
  When they try to access any project, team or task
  Then access is refused
  And the message "Please log in to continue." is shown
  And they are asked to log in
```

### DoD03 — Access only to projects the user is a member of (NFR03)

```gherkin
Scenario: A user tries to access a project they are not a member of
  Given the user "Ana" is not a member of the project "Website"
  When she tries to view or change the project "Website" or any of its tasks
  Then access is refused
  And the message "Project not found." is shown
  And no project data is shown or changed
```

The message is the same as for a project that doesn't exist. This way, users can't find out which projects exist.

### DoD04 — Exact messages

Quoted messages in the acceptance criteria are the exact text to show. When an action is refused by a validation, no data is created or changed.

### DoD05 — Task that doesn't exist

Applies to every user story that acts on an existing task (US07, US08, US10, US13, US16).

```gherkin
Scenario: A member acts on a task that doesn't exist in the project
  Given Ana is a member of the project "Website"
  And the project "Website" has no task with the given identifier
  When she tries to view, edit, change the status of or assign that task
  Then the message "Task not found." is shown
  And no data is changed
```

### DoD06 — Request that can't be verified

Applies to every action that changes data while logged in. It protects the user against actions sent from other websites on their behalf.

```gherkin
Scenario: A change request without a valid verification token
  Given Ana is logged in
  When a request to change data arrives without the verification token of her session, or with a wrong one
  Then the request is refused
  And the message "The request could not be verified. Please reload and try again." is shown
  And no data is changed
```

### DoD07 — Malformed request

```gherkin
Scenario: A request with malformed data
  When a request arrives whose data can't be read, has unknown fields or has fields of the wrong type
  Then the request is refused
  And the message "The request body is not valid." is shown
  And no data is created or changed
```

## 3. Decisions made to remove ambiguities

The requirements left some points open. These decisions apply to every US.

| # | Decision |
|---|---|
| D01 | **Leading and trailing spaces:** text fields are saved without leading or trailing spaces. A required field containing only spaces counts as empty. |
| D02 | **Length limits:** limits count characters after applying D01. |
| D03 | **Emails:** compared case-insensitively ("Ana@Example.com" and "ana@example.com" are the same email). |
| D04 | **Adding people:** people are added to projects and teams by their account email. |
| D05 | **Teams visible to everyone:** every user sees the list of teams, with each team's name and members, so they can assign tasks to them. |
| D06 | **Names:** project names can repeat, but team names are unique, because teams are chosen by name when assigning tasks. |
| D07 | **Managers are also members:** whoever creates a project or team becomes its manager and a member. The manager can't be removed. |
| D08 | **Any member can change tasks:** any project member can edit, change the status of and assign any task in that project, not just the assignee. |
| D09 | **Team assignment is only checked when assigning:** the rule "all team members belong to the project" is only checked when the task is assigned. Later changes to the team don't undo existing assignments. |
| D10 | **Removing a project member:** tasks assigned to that person in that project become unassigned. Tasks assigned to teams are not affected. |
| D11 | **Simultaneous changes:** if two members change the same task at the same time, the last saved change wins. |
| D12 | **Due dates:** a new or changed due date can't be earlier than the current day. A past due date that isn't changed stays valid. |

## 4. Traceability

Every MVP requirement is covered by at least one US or by a cross-cutting criterion.

| Requirement or constraint | Covered by |
|---|---|
| FR01 | US01 |
| FR02 | US02 |
| FR03 | US03 |
| FR04 | US14 |
| FR05 | US15 |
| FR06 | US09 |
| FR07 | US04 |
| FR08 | US11 |
| FR09 | US12 |
| FR19 | US17 |
| FR20 | US18 |
| FR10 | US05 |
| FR11 | US08 |
| FR12 | US16 |
| FR13 | US07 |
| FR14 | US10, US13 |
| FR15 | US06 |
| NFR02 | US02, DoD02 |
| NFR03 | US04, US09, DoD03 |
| NFR04 | US01 |
| NFR05 | US15, US16, US18 |
| NFR07 | US07 |
| C05 | DoD01 |

The Should requirements (FR16, FR17, FR18, NFR06) are not in this backlog and will be turned into user stories after the MVP.

## 5. User stories

Each story has a priority, an implementation order, the requirements it covers, its acceptance criteria in Given / When / Then form, and its exceptional situations and edge cases.

### US01 — Register an account

| Priority | Order | Requirements | Depends on |
|---|---|---|---|
| Must (MVP) | 1 | FR01, NFR04 | — |

#### User story

**As a** visitor,
**I want** to create an account with my name, email and password,
**so that** I can take part in Matriz projects.

#### Rules

| Field | Rules |
|---|---|
| Name | Required, between 1 and 100 characters. |
| Email | Required, in the format `text@domain.extension`, at most 254 characters. Must be unique (case-insensitive, D03). |
| Password | Required, between 8 and 128 characters. Spaces are not trimmed. |

#### Acceptance criteria

##### Main scenario

```gherkin
Scenario: AC01 — Successful sign-up
  Given there is no account with the email "ana@example.com"
  When the visitor signs up with the name "Ana Silva", the email "ana@example.com" and the password "secure123"
  Then an account is created with the name "Ana Silva" and the email "ana@example.com"
  And the visitor is logged in with that account
  And the project list is shown (US04)
```

##### Exceptional situations

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

##### Edge cases

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

##### Password security (NFR04)

```gherkin
Scenario: AC09 — Password hidden while typing
  When the visitor types the password in the sign-up form
  Then each character is shown as a mask symbol, not as the typed character

Scenario: AC10 — Password not stored in readable text
  Given the visitor signed up with the password "secure123"
  When the stored data for that account is inspected
  Then the text "secure123" does not appear in any of the stored data
  And decoding the stored value as Base64 or hexadecimal does not produce "secure123"
  And a second account created with the password "secure123" stores a different value
```

### US02 — Log in and log out

| Priority | Order | Requirements | Depends on |
|---|---|---|---|
| Must (MVP) | 2 | FR02, NFR02 | US01 |

#### User story

**As a** registered user,
**I want** to log in and log out with my email and password,
**so that** I can access my projects without anyone else doing so on my behalf.

#### Acceptance criteria

Shared context for all scenarios: there is an account with the email "ana@example.com" and the password "secure123".

##### Main scenario

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

##### Exceptional situations

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

##### Edge cases

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

### US03 — Create a project

| Priority | Order | Requirements | Depends on |
|---|---|---|---|
| Must (MVP) | 3 | FR03 | US02 |

#### User story

**As a** user,
**I want** to create a project with a name and a description,
**so that** I can gather a piece of work's tasks in one place.

#### Rules

| Field | Rules |
|---|---|
| Name | Required, between 1 and 100 characters. Can repeat (D06). |
| Description | Optional, up to 2000 characters. |

Whoever creates the project becomes its manager and a member (D07).

#### Acceptance criteria

##### Main scenario

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

##### Exceptional situations

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

##### Edge cases

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

### US04 — View my projects

| Priority | Order | Requirements | Depends on |
|---|---|---|---|
| Must (MVP) | 4 | FR07, NFR03 | US03 |

#### User story

**As a** user,
**I want** to see the list of projects I am a member of,
**so that** I can quickly get to the work in each one.

#### Rules

- For each project, the list shows the name and the user's role: "Manager" or "Member".
- The list is sorted alphabetically by name, case-insensitively.

#### Acceptance criteria

##### Main scenario

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

##### Exceptional situations

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

##### Edge cases

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

### US05 — Create a task

| Priority | Order | Requirements | Depends on |
|---|---|---|---|
| Must (MVP) | 5 | FR10 | US03 |

#### User story

**As a** project member,
**I want** to create a task in the project,
**so that** I can record work that needs to be done.

#### Rules

| Field | Rules |
|---|---|
| Title | Required, between 1 and 200 characters. |
| Description | Optional, up to 5000 characters. |
| Due date | Optional. Must be a valid date and can't be earlier than the current day (D12). |

A new task always starts in the "To do" status and with no assignee ("Unassigned").

#### Acceptance criteria

Shared context for all scenarios: Ana is a member of the project "Website" and today is 2026-11-02.

##### Main scenario

```gherkin
Scenario: AC01 — Create a task with every field
  When Ana creates a task in the project "Website" with:
    | title       | Design home page        |
    | description | Follow the style guide  |
    | due date    | 2026-11-20              |
  Then the task "Design home page" is created in the project "Website" with that data
  And the task status is "To do"
  And the task has no assignee ("Unassigned")
  And the task appears in the project's task list (US06)

Scenario: AC02 — Create a task with only a title
  When Ana creates a task in the project "Website" with only the title "Review copy"
  Then the task "Review copy" is created with no description and no due date
```

##### Exceptional situations

```gherkin
Scenario Outline: AC03 — Missing title
  When Ana creates a task with the title <title>
  Then the message "Task title is required." is shown
  And no task is created

  Examples:
    | title |
    | ""    |
    | "   " |

Scenario: AC04 — Due date in the past
  When Ana creates a task with the due date 2026-11-01
  Then the message "Due date cannot be in the past." is shown
  And no task is created

Scenario: AC05 — Non-existent due date
  When Ana creates a task with the due date 2027-02-30
  Then the message "Enter a valid date." is shown
  And no task is created

Scenario: AC06 — Create a task in a project I'm not a member of
  Given Rui is not a member of the project "Website"
  When Rui tries to create a task in the project "Website"
  Then the message "Project not found." is shown
  And no task is created
```

##### Edge cases

```gherkin
Scenario: AC07 — Due date equal to the current day
  When Ana creates a task with the due date 2026-11-02
  Then the task is created with the due date 2026-11-02

Scenario Outline: AC08 — Title and description limits
  When Ana creates a task with a <field> of <n> characters
  Then the result is <result>

  Examples:
    | field       | n    | result                                                      |
    | title       | 200  | the task is created                                         |
    | title       | 201  | the message "Task title must be at most 200 characters."    |
    | description | 5000 | the task is created                                         |
    | description | 5001 | the message "Description must be at most 5000 characters."  |

Scenario: AC09 — Repeated titles in the same project
  Given the task "Review copy" already exists in the project "Website"
  When Ana creates another task with the title "Review copy" in the same project
  Then a second task is created, separate from the first
```

### US06 — View a project's task board

| Priority | Order | Requirements | Depends on |
|---|---|---|---|
| Must (MVP) | 6 | FR15 | US05 |

#### User story

**As a** project member,
**I want** to see all the project's tasks with each one's status and assignee,
**so that** I have an overview of the work and its progress.

#### Rules

- Each task shows its title, status, assignee and due date (if any).
- The assignee is shown as:
  - the person's name;
  - the team's name, followed by "(team)";
  - "Unassigned", if there is no assignee.
- Tasks are shown as a board with one column per status, in this order: "To do", "In progress", "Done".
- Each column shows the number of tasks in it.
- Within each column, tasks are sorted from newest to oldest by creation date.
- The board shows every task in the project, including done ones.

#### Acceptance criteria

Shared context for all scenarios: Ana is a member of the project "Website".

##### Main scenario

```gherkin
Scenario: AC01 — See the project's tasks
  Given the project "Website" has these tasks, created in this order:
    | title         | status      | assignee  | due date   |
    | Review copy   | Done        | Rui Costa |            |
    | Create logo   | In progress | Design    | 2026-11-20 |
    | Test the site | To do       |           |            |
  And "Design" is a team
  When Ana opens the project "Website"
  Then the board shows the columns "To do", "In progress" and "Done", in this order, each with 1 task
  And the column "To do" shows "Test the site", assigned to "Unassigned", with no due date
  And the column "In progress" shows "Create logo", assigned to "Design (team)", with the due date 2026-11-20
  And the column "Done" shows "Review copy", assigned to "Rui Costa", with no due date

Scenario: AC06 — Newest first within a column
  Given the project "Website" has the tasks "Write the privacy page" and then "Test the site", both in "To do"
  When Ana opens the project "Website"
  Then the column "To do" shows "Test the site" above "Write the privacy page"
```

AC01 was changed and AC06 added when the task list became a board (see the [UI wireframe](ui/README.md)). AC06 comes after AC05 in numbering.

##### Exceptional situations

```gherkin
Scenario: AC02 — Project with no tasks
  Given the project "Website" has no tasks
  When Ana opens the project "Website"
  Then the message "No tasks yet." is shown

Scenario: AC03 — Project I'm not a member of
  Given Rui is not a member of the project "Website"
  When Rui tries to see the tasks of the project "Website"
  Then the message "Project not found." is shown
  And no task is shown
```

##### Edge cases

```gherkin
Scenario: AC04 — Only the open project's tasks are shown
  Given Ana is a member of the projects "Website" and "App"
  And the project "App" has the task "Publish to the store"
  When Ana opens the project "Website"
  Then the task "Publish to the store" is not shown

Scenario: AC05 — The list reflects other members' changes (NFR07)
  Given Rui changed the status of the task "Test the site" to "In progress"
  When Ana opens the project "Website" after that change
  Then the task "Test the site" is shown with the status "In progress"
```

### US07 — Change a task's status

| Priority | Order | Requirements | Depends on |
|---|---|---|---|
| Must (MVP) | 7 | FR13, NFR07 | US05 |

#### User story

**As a** project member,
**I want** to change a task's status,
**so that** everyone knows where the work stands.

#### Rules

- The possible statuses are "To do", "In progress" and "Done".
- Any transition between statuses is allowed, including going back.
- Any project member can change the status of any task in the project, even if they aren't the assignee (D08).
- Changing the status doesn't change any other field of the task.

#### Acceptance criteria

Shared context for all scenarios: Ana and Rui are members of the project "Website", which has the task "Test the site" in the "To do" status.

##### Main scenario

```gherkin
Scenario: AC01 — Change a task's status
  When Ana changes the status of the task "Test the site" to "In progress"
  Then the task status becomes "In progress"
  And the title, description, due date and assignee don't change

Scenario Outline: AC02 — Every transition is allowed
  Given the task "Test the site" is in the status <from>
  When Ana changes the status to <to>
  Then the task status becomes <to>

  Examples:
    | from        | to          |
    | To do       | In progress |
    | To do       | Done        |
    | In progress | To do       |
    | In progress | Done        |
    | Done        | To do       |
    | Done        | In progress |

Scenario: AC03 — The change is visible to other members (NFR07)
  Given Ana changed the status of the task "Test the site" to "Done"
  When Rui opens the project "Website" after that change
  Then Rui sees the task "Test the site" with the status "Done"
```

##### Exceptional situations

```gherkin
Scenario: AC04 — Invalid status
  When the status of the task "Test the site" is requested to become "Blocked"
  Then the message "Invalid status." is shown
  And the task status stays "To do"

Scenario: AC05 — User who is not a project member
  Given Marta is not a member of the project "Website"
  When Marta tries to change the status of the task "Test the site"
  Then the message "Project not found." is shown
  And the task status stays "To do"
```

##### Edge cases

```gherkin
Scenario: AC06 — Choose the current status
  When Ana changes the status of the task "Test the site" to "To do"
  Then the task status stays "To do"
  And no error message is shown

Scenario: AC07 — Member who is not the assignee
  Given the task "Test the site" is assigned to Rui
  When Ana changes the task status to "Done"
  Then the task status becomes "Done"

Scenario: AC08 — Two simultaneous changes (D11)
  Given Ana and Rui both have the task "Test the site" open
  When Ana changes the status to "In progress"
  And then Rui changes the status to "Done"
  Then the task's final status is "Done"
```

### US08 — Edit a task

| Priority | Order | Requirements | Depends on |
|---|---|---|---|
| Must (MVP) | 8 | FR11 | US05 |

#### User story

**As a** project member,
**I want** to edit a task's title, description and due date,
**so that** I can keep the task's information correct without recreating it.

#### Rules

- Field validations are the same as in [US05](#us05--create-a-task).
- Editing doesn't change the task's status or assignee.
- The description and due date can be cleared.
- Any project member can edit any task in the project (D08).

#### Acceptance criteria

Shared context for all scenarios: Ana is a member of the project "Website", today is 2026-11-02 and the project has this task:

| title | description | due date | status | assignee |
|---|---|---|---|---|
| Test the site | Test on mobile | 2026-11-20 | In progress | Rui Costa |

##### Main scenario

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

##### Exceptional situations

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

##### Edge cases

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

### US09 — Manage a project's members

| Priority | Order | Requirements | Depends on |
|---|---|---|---|
| Must (MVP) | 9 | FR06, NFR03 | US03 |

#### User story

**As a** project manager,
**I want** to add and remove project members,
**so that** the right people can work on it.

#### Rules

- Members are added by their account email (D04).
- Every member sees the project's member list, with each member's name, email and role ("Manager" or "Member").
- Only the manager can add and remove members.
- The manager can't be removed (D07).
- When a member is removed, the tasks assigned to that person in the project become unassigned. Tasks assigned to teams don't change (D10).

#### Acceptance criteria

Shared context for all scenarios: Ana is the manager of the project "Website" and Rui has an account with the email "rui@example.com".

##### Main scenario

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

##### Exceptional situations

```gherkin
Scenario: AC04 — Email without an account
  When Ana adds the email "marta@example.com", which has no account
  Then the message "This user could not be added." is shown
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

Scenario: AC10 — Email missing
  When Ana adds a member without giving an email
  Then the message "Email is required." is shown
  And the project's members don't change

Scenario: AC11 — Remove someone who isn't a member
  Given Marta has an account but isn't a member of the project "Website"
  When Ana tries to remove Marta from the project "Website"
  Then the message "Member not found." is shown
  And the project's members don't change
```

AC10 and AC11 were added after the first version of the backlog, so they come after AC09 in numbering.

##### Edge cases

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

### US10 — Assign a task to a person

| Priority | Order | Requirements | Depends on |
|---|---|---|---|
| Must (MVP) | 10 | FR14 | US05, US09 |

#### User story

**As a** project member,
**I want** to assign a task to a project member, or undo that assignment,
**so that** it's clear who is responsible for it.

#### Rules

- A task has at most one assignee: a person or a team ([US13](#us13--assign-a-task-to-a-team)).
- A task can only be assigned to a person who is a project member.
- Assigning a task that already has an assignee replaces the previous assignee.
- Any project member can assign any task in the project (D08).

#### Acceptance criteria

Shared context for all scenarios: Ana and Rui are members of the project "Website", which has the task "Test the site" with no assignee.

##### Main scenario

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

##### Exceptional situations

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

##### Edge cases

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

### US11 — Create a team

| Priority | Order | Requirements | Depends on |
|---|---|---|---|
| Must (MVP) | 11 | FR08 | US02 |

#### User story

**As a** user,
**I want** to create a team,
**so that** I can assign tasks to a group of people instead of just one.

#### Rules

| Field | Rules |
|---|---|
| Name | Required, between 1 and 50 characters. Unique across all teams, case-insensitively (D06). |

- Whoever creates the team becomes its manager and a member (D07).
- Every user sees the list of teams, with each team's name and members (D05).

#### Acceptance criteria

Shared context for all scenarios: Ana is logged in.

##### Main scenario

```gherkin
Scenario: AC01 — Create a team
  Given there is no team called "Design"
  When Ana creates the team "Design"
  Then the team "Design" is created
  And Ana becomes the team's manager and a member
  And the team "Design" appears in every user's team list
```

##### Exceptional situations

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

##### Edge cases

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

### US12 — Manage a team's members

| Priority | Order | Requirements | Depends on |
|---|---|---|---|
| Must (MVP) | 12 | FR09 | US11 |

#### User story

**As a** team manager,
**I want** to add and remove people from the team,
**so that** the team reflects who works together.

#### Rules

- People are added by their account email (D04).
- Only the team manager can add and remove members.
- The manager can't be removed (D07).
- Changing a team's members doesn't undo task assignments already made to that team (D09).

#### Acceptance criteria

Shared context for all scenarios: Ana is the manager of the team "Design" and Rui has an account with the email "rui@example.com".

##### Main scenario

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

##### Exceptional situations

```gherkin
Scenario: AC03 — Email without an account
  When Ana adds the email "marta@example.com", which has no account
  Then the message "This user could not be added." is shown
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

Scenario: AC09 — Email missing
  When Ana adds a member without giving an email
  Then the message "Email is required." is shown
  And the team's members don't change

Scenario: AC10 — Remove someone who isn't a member
  Given Marta has an account but isn't a member of the team "Design"
  When Ana tries to remove Marta from the team "Design"
  Then the message "Member not found." is shown
  And the team's members don't change

Scenario: AC11 — Team that doesn't exist
  When Ana tries to add or remove a member of a team that doesn't exist
  Then the message "Team not found." is shown
```

AC09 to AC11 were added after the first version of the backlog, so they come after AC08 in numbering.

##### Edge cases

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

### US13 — Assign a task to a team

| Priority | Order | Requirements | Depends on |
|---|---|---|---|
| Must (MVP) | 13 | FR14 | US10, US12 |

#### User story

**As a** project member,
**I want** to assign a task to a team,
**so that** a group of people is jointly responsible for it.

#### Rules

- A task has at most one assignee: a person ([US10](#us10--assign-a-task-to-a-person)) or a team.
- A task can only be assigned to a team whose members are all project members. This rule is only checked at the moment of assignment (D09).
- Assigning replaces the previous assignee, whether a person or another team.
- An assignment to a team is undone the same way as an assignment to a person (US10, AC03).

#### Acceptance criteria

Shared context for all scenarios: Ana, Rui and Sara are members of the project "Website", and the team "Design" has Rui and Sara as members.

##### Main scenario

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

##### Exceptional situations

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

##### Edge cases

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

### US14 — Edit a project

| Priority | Order | Requirements | Depends on |
|---|---|---|---|
| Must (MVP) | 14 | FR04, NFR03 | US03 |

#### User story

**As a** project manager,
**I want** to edit my project's name and description,
**so that** the project's information stays correct and up to date.

#### Rules

- The field validations are the same as in [US03](#us03--create-a-project).
- Only the project manager can edit the project (NFR03).
- The description can be cleared.

#### Acceptance criteria

Shared context for all scenarios: Ana is the manager of the project "Website", whose description is "New company website", and Rui is a member of it.

##### Main scenario

```gherkin
Scenario: AC01 — Edit the name and description
  When Ana edits the project "Website" to the name "Company website" and the description "Public site and blog"
  Then the project has the name "Company website" and the description "Public site and blog"
  And Rui sees the new name in his project list the next time he opens it (NFR07)

Scenario: AC02 — Clear the description
  When Ana clears the description of the project "Website"
  Then the project has an empty description
```

##### Exceptional situations

```gherkin
Scenario: AC03 — Name cleared
  When Ana edits the name of the project "Website" to ""
  Then the message "Project name is required." is shown
  And the project keeps its previous name and description

Scenario: AC04 — A member who isn't the manager tries to edit (NFR03)
  When Rui tries to edit the project "Website"
  Then the message "Only the project manager can edit the project." is shown
  And the project doesn't change

Scenario: AC05 — User who is not a project member
  Given Marta is not a member of the project "Website"
  When Marta tries to edit the project "Website"
  Then the message "Project not found." is shown
  And the project doesn't change
```

##### Edge cases

```gherkin
Scenario Outline: AC06 — Name limits
  When Ana edits the name of the project to a name of <n> characters
  Then the result is <result>

  Examples:
    | n   | result                                                      |
    | 100 | the change is saved                                         |
    | 101 | the message "Project name must be at most 100 characters."  |

Scenario: AC07 — Save without changes
  When Ana saves the project "Website" without changing any field
  Then the project keeps all its data
  And no error message is shown

Scenario: AC08 — Name used by another project (D06)
  Given another project is called "App"
  When Ana renames the project "Website" to "App"
  Then the change is saved
```

### US15 — Delete a project

| Priority | Order | Requirements | Depends on |
|---|---|---|---|
| Must (MVP) | 15 | FR05, NFR03, NFR05 | US03, US05 |

#### User story

**As a** project manager,
**I want** to delete a project I no longer need,
**so that** the project list only shows projects that are still in use.

#### Rules

- Only the project manager can delete the project (NFR03).
- Before deleting, the user must confirm (NFR05). The confirmation shows the message `Delete "<project name>" and all its tasks? This can't be undone.` with two choices: "Delete project" and "Cancel".
- Deleting a project also deletes all its tasks and all its memberships, in one operation (FR05). Teams aren't affected, even if tasks were assigned to them.
- After the deletion, the project is no longer accessible to anyone: it gets "Project not found." (DoD03).

#### Acceptance criteria

Shared context for all scenarios: Ana is the manager of the project "Website", which has the tasks "Test the site" and "Create logo", and Rui is a member of it.

##### Main scenario

```gherkin
Scenario: AC01 — Confirmation before deleting (NFR05)
  When Ana chooses to delete the project "Website"
  Then the message 'Delete "Website" and all its tasks? This can't be undone.' is shown
  And the choices "Delete project" and "Cancel" are shown
  And nothing is deleted yet

Scenario: AC02 — Delete a project
  When Ana chooses to delete the project "Website" and confirms with "Delete project"
  Then the project "Website" and its tasks "Test the site" and "Create logo" are deleted
  And the project no longer appears in Ana's or Rui's project list
  And opening the project "Website" shows "Project not found."

Scenario: AC03 — Cancel the deletion
  When Ana chooses to delete the project "Website" and then chooses "Cancel"
  Then the project and its tasks are not deleted
```

##### Exceptional situations

```gherkin
Scenario: AC04 — A member who isn't the manager tries to delete (NFR03)
  When Rui tries to delete the project "Website"
  Then the message "Only the project manager can delete the project." is shown
  And nothing is deleted

Scenario: AC05 — User who is not a project member
  Given Marta is not a member of the project "Website"
  When Marta tries to delete the project "Website"
  Then the message "Project not found." is shown
  And nothing is deleted
```

##### Edge cases

```gherkin
Scenario: AC06 — Tasks assigned to a team
  Given the task "Create logo" is assigned to the team "Design"
  When Ana deletes the project "Website" and confirms
  Then the team "Design" still exists, with the same members

Scenario: AC07 — Project without tasks
  Given the project "Intranet" has no tasks and Ana is its manager
  When Ana deletes the project "Intranet" and confirms
  Then the project "Intranet" is deleted
```

### US16 — Delete a task

| Priority | Order | Requirements | Depends on |
|---|---|---|---|
| Must (MVP) | 16 | FR12, NFR05 | US05 |

#### User story

**As a** project member,
**I want** to delete a task that was created by mistake or is no longer needed,
**so that** the project's board only shows real work.

#### Rules

- Any project member can delete any task of the project (D08).
- Before deleting, the user must confirm (NFR05). The confirmation shows the message `Delete "<task title>"? This can't be undone.` with two choices: "Delete task" and "Cancel".
- A deleted task disappears from the board for every member (NFR07).

#### Acceptance criteria

Shared context for all scenarios: Ana and Rui are members of the project "Website", which has the tasks "Test the site" and "Create logo", both in "To do".

##### Main scenario

```gherkin
Scenario: AC01 — Confirmation before deleting (NFR05)
  When Ana chooses to delete the task "Test the site"
  Then the message 'Delete "Test the site"? This can't be undone.' is shown
  And the choices "Delete task" and "Cancel" are shown
  And nothing is deleted yet

Scenario: AC02 — Delete a task
  When Ana chooses to delete the task "Test the site" and confirms with "Delete task"
  Then the task "Test the site" no longer appears on the board
  And the column "To do" shows 1 task
  And Rui no longer sees the task the next time he opens the project (NFR07)

Scenario: AC03 — Cancel the deletion
  When Ana chooses to delete the task "Test the site" and then chooses "Cancel"
  Then the task "Test the site" still appears on the board
```

##### Exceptional situations

```gherkin
Scenario: AC04 — User who is not a project member
  Given Marta is not a member of the project "Website"
  When Marta tries to delete the task "Test the site"
  Then the message "Project not found." is shown
  And the task is not deleted

Scenario: AC05 — Task already deleted by another member (DoD05)
  Given Rui deleted the task "Test the site"
  When Ana, who still had it open, confirms its deletion
  Then the message "Task not found." is shown
```

##### Edge cases

```gherkin
Scenario: AC06 — Member who is not the assignee (D08)
  Given the task "Create logo" is assigned to Rui
  When Ana deletes the task "Create logo" and confirms
  Then the task is deleted

Scenario: AC07 — Delete the last task
  Given the project "Website" has only the task "Test the site"
  When Ana deletes it and confirms
  Then the board shows "No tasks yet."
```

### US17 — Rename a team

| Priority | Order | Requirements | Depends on |
|---|---|---|---|
| Must (MVP) | 17 | FR19, NFR03 | US11 |

#### User story

**As a** team manager,
**I want** to rename my team,
**so that** its name stays correct when the team changes.

#### Rules

- The name rules are the same as in [US11](#us11--create-a-team): 1 to 50 characters, unique among all teams ignoring case (D06).
- Only the team manager can rename the team (NFR03).
- Tasks assigned to the team keep their assignment and show the new name.

#### Acceptance criteria

Shared context for all scenarios: Ana is the manager of the team "Design", Rui is a member of it, and the team "QA" also exists.

##### Main scenario

```gherkin
Scenario: AC01 — Rename a team
  Given the task "Create logo" is assigned to the team "Design"
  When Ana renames the team "Design" to "Product design"
  Then the team is called "Product design"
  And the task "Create logo" shows "Product design (team)" as its assignee
```

##### Exceptional situations

```gherkin
Scenario: AC02 — Name already used by another team
  When Ana renames the team "Design" to "qa"
  Then the message "A team with this name already exists." is shown
  And the team keeps the name "Design"

Scenario: AC03 — Name cleared
  When Ana renames the team "Design" to ""
  Then the message "Team name is required." is shown
  And the team keeps the name "Design"

Scenario: AC04 — A member who isn't the manager tries to rename (NFR03)
  When Rui tries to rename the team "Design"
  Then the message "Only the team manager can edit the team." is shown
  And the team keeps the name "Design"

Scenario: AC05 — Team that doesn't exist
  When Ana tries to rename a team that doesn't exist
  Then the message "Team not found." is shown
```

##### Edge cases

```gherkin
Scenario: AC06 — Change only the casing of the team's own name
  When Ana renames the team "Design" to "DESIGN"
  Then the team is called "DESIGN"

Scenario: AC07 — Name too long
  When Ana renames the team "Design" to a name of 51 characters
  Then the message "Team name must be at most 50 characters." is shown
```

### US18 — Delete a team

| Priority | Order | Requirements | Depends on |
|---|---|---|---|
| Must (MVP) | 18 | FR20, NFR03, NFR05 | US11, US13 |

#### User story

**As a** team manager,
**I want** to delete a team that no longer exists,
**so that** nobody assigns new work to it.

#### Rules

- Only the team manager can delete the team (NFR03).
- Before deleting, the user must confirm (NFR05). The confirmation shows the message `Delete the team "<team name>"? Its tasks will become unassigned. This can't be undone.` with two choices: "Delete team" and "Cancel".
- Deleting a team deletes its memberships. Every task assigned to the team, in any project, becomes unassigned, in the same operation (FR20).
- The team's members keep their accounts and their project memberships.

#### Acceptance criteria

Shared context for all scenarios: Ana is the manager of the team "Design", whose members are Ana and Rui. The task "Create logo" of the project "Website" is assigned to the team "Design".

##### Main scenario

```gherkin
Scenario: AC01 — Confirmation before deleting (NFR05)
  When Ana chooses to delete the team "Design"
  Then the message 'Delete the team "Design"? Its tasks will become unassigned. This can't be undone.' is shown
  And the choices "Delete team" and "Cancel" are shown
  And nothing is deleted yet

Scenario: AC02 — Delete a team
  When Ana chooses to delete the team "Design" and confirms with "Delete team"
  Then the team "Design" no longer appears in the team list
  And the task "Create logo" becomes unassigned ("Unassigned")
  And Rui is still a member of the project "Website"

Scenario: AC03 — Cancel the deletion
  When Ana chooses to delete the team "Design" and then chooses "Cancel"
  Then the team "Design" still exists
  And the task "Create logo" stays assigned to the team "Design"
```

##### Exceptional situations

```gherkin
Scenario: AC04 — A member who isn't the manager tries to delete (NFR03)
  When Rui tries to delete the team "Design"
  Then the message "Only the team manager can delete the team." is shown
  And nothing is deleted

Scenario: AC05 — Team that doesn't exist
  When Ana tries to delete a team that doesn't exist
  Then the message "Team not found." is shown
```

##### Edge cases

```gherkin
Scenario: AC06 — Team without tasks
  Given the team "QA" has no tasks assigned and Ana is its manager
  When Ana deletes the team "QA" and confirms
  Then the team "QA" is deleted

Scenario: AC07 — The name becomes available again
  Given Ana deleted the team "Design"
  When any user creates a team called "Design"
  Then the new team is created
```
