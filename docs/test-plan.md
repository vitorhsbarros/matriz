# Matriz — Test plan: tasks and their permissions

This plan defines acceptance tests for tasks and their permissions **from the requirements**, not from the current behaviour of the implementation. Every expected result comes from a source of truth and cites it. The code, the existing tests and the API contract were not used, so a test that fails points to a deviation from the specification rather than recording what the code happens to do.

## 1. Scope

**Covered:**
- creating a task (US05), with its field rules and the decisions that apply to them (D01, D02, D12);
- reading tasks on the board (US06) and in the details (US20);
- who can create, read and change a task (US07, US08 and US16, for permissions only), and how access is refused (DoD02, DoD03, DoD05, DoD06, DoD07, SEC01 to SEC03, SEC07).

**Left out**, because they concern tasks but not creating them, reading them or their permissions:
- assignees (US10, US13);
- comments (US21, US22);
- filters (US24);
- simultaneous changes (D11);
- the full behaviour of editing, changing the status and deleting (US07, US08, US16, apart from their permission scenarios).

**Deliverable:** this plan only. No test code is written here.

## 2. Sources

Only the sources of truth were used:

| Document | Criteria used |
|---|---|
| [user-stories.md](user-stories.md) | US05 AC01–AC10 and Rules; US06 AC01, AC03; US07 AC05, AC07; US08 AC05, AC06; US16 AC04, AC06; US20 AC01–AC03; DoD02–DoD07; D01, D02, D08, D12 |
| [constraints.md](constraints.md) | SEC01, SEC02, SEC03, SEC07 |
| [ADR-0002](architecture/adr/ADR-0002-session-cookie-authentication.md) | Session and verification (CSRF) token, through SEC07 and DoD06 |

**Not used:** `openapi.yaml`, [api-contract.md](api-contract.md), production code and existing tests. As a result, expected results are stated as the outcome and message the specification gives, not as status codes or paths. Status codes, paths and field names are left to the integration tests, which take them from the API contract.

## 3. Scenario matrix

**Levels:**
- **unit:** a black-box test of a single field rule, with no persistence;
- **integration/API:** a request to the running application with a real database, checking the answer and what was stored;
- **end-to-end (UI):** the browser application, as a user sees it.

Shared context unless the scenario says otherwise:
- Ana is a member of the project "Website";
- Rui and Marta are not members of it;
- today is 2026-11-02.

| ID | Scenario | Inputs | Expected result | Level | Source |
|---|---|---|---|---|---|
| TP01 | Create a task with every field | Ana; title "Design home page", description "Follow the style guide", due date 2026-11-20, priority "High" | Task created with that data, status "To do", no assignees ("Unassigned") | integration/API | US05 AC01 |
| TP02 | Create a task with only a title | Ana; title "Review copy" | Created with no description, no due date, priority "Medium", status "To do" | integration/API | US05 AC02, US05 Rules |
| TP03 | Missing title | Title "" and title "   " | "Task title is required." | unit | US05 AC03, D01 |
| TP04 | Title length limits, counted after trimming | Titles of 2, 3, 150 and 151 characters; title "  ab  "; title "  Review copy  " | 2 → "Task title must be at least 3 characters."; 3 and 150 → accepted; 151 → "Task title must be at most 150 characters."; "  ab  " → "Task title must be at least 3 characters."; "  Review copy  " → saved as "Review copy" | unit | US05 AC08, D01, D02 |
| TP05 | Several invalid fields at once | Ana; title "" and a description of 2001 characters | The task is refused with one of the two fields' messages ("Task title is required." or "Description must be at most 2000 characters."), and no task is created. Which message is shown is not asserted (see section 6, G1) | integration/API | DoD04, US05 AC03, US05 AC08, US08 AC05 |
| TP06 | An invalid title creates nothing | Ana; title "ab" | "Task title must be at least 3 characters.", and the project's tasks are unchanged | integration/API | US05 AC08, DoD04 |
| TP07 | Description limit | Descriptions of 2000 and 2001 characters | 2000 → accepted; 2001 → "Description must be at most 2000 characters." | unit | US05 AC08 |
| TP08 | Due date rules | Due dates 2026-11-01, 2026-11-02 and 2027-02-30 | "Due date cannot be in the past."; accepted; "Enter a valid date." | unit | US05 AC04, AC05, AC07, D12 |
| TP09 | Invalid priority | Ana; priority "Urgent" | "Invalid priority.", and no task created | integration/API | US05 AC10 |
| TP10 | Create a task, then read it | Ana creates "Design home page" with every field (as TP01), then opens it from the board | The card is in the column "To do", shows priority "High", "Unassigned" and the due date 2026-11-20; its details show the title, description, status, priority and due date that were entered | end-to-end (UI) | US05 AC01, US06 AC01, US20 AC01 |
| TP11 | Read a task's details | Ana; the task "Create logo" with description, status, priority, due date, assignees and a comment | All of them are returned | integration/API | US20 AC01 |
| TP12 | A non-member creates a task | Rui creates a task in "Website" | "Project not found.", and no task created | integration/API | US05 AC06, DoD03, SEC02 |
| TP13 | A non-member reads tasks | Marta reads the board of "Website" and the task "Create logo" | "Project not found.", and no task data shown | integration/API | US06 AC03, US20 AC02, DoD03, SEC02 |
| TP14 | A non-member changes a task | Marta edits, changes the status of and deletes the task "Test the site" | "Project not found." each time, and the task is unchanged | integration/API | US07 AC05, US08 AC06, US16 AC04 |
| TP15 | An inaccessible project looks like a missing one | Rui reads "Website" (exists, not a member) and a project identifier that doesn't exist | The two answers are identical: "Project not found." | integration/API | SEC03, DoD03 |
| TP16 | A visitor who isn't logged in | No session; create a task in "Website", and read its board | Access refused, "Please log in to continue." | integration/API | DoD02, SEC01 |
| TP17 | A change without a verification token | Ana, logged in; create a task without her session's token, and with a wrong one | "The request could not be verified. Please reload and try again.", and no task created | integration/API | DoD06, SEC07, ADR-0002 |
| TP18 | A member who isn't an assignee changes the task | Ana; a task assigned only to Rui, who is a member; change its status, edit it, delete it | Each action succeeds | integration/API | D08, US07 AC07, US16 AC06 |
| TP19 | A task that doesn't exist in the project | Ana; the identifier of a deleted task, and of a task in another project she is a member of | "Task not found.", and no data changed | integration/API | DoD05, US20 AC03 |
| TP20 | Malformed request | Ana creates a task with an unknown field, and with a title that is a number | "The request body is not valid.", and no task created | integration/API | DoD07 |

## 4. Selected scenarios

These five are the essential ones: if any of them fails, the most important task and permission rules are broken.

| ID | Why it was chosen | Why the expected result is what it is |
|---|---|---|
| **TP06** — invalid title | It covers the invalid title through the whole application, not just the rule. It checks both the message and that nothing is stored, which a unit test can't. | US05 AC08 gives the exact message for 2 characters, and DoD04 says that a refused validation creates no data. |
| **TP10** — create and read a task | It is the main flow as a user sees it: what is created must be what appears on the board and in the details. | US05 AC01 fixes the initial status "To do" and "Unassigned". US06 AC01 fixes what the card shows. US20 AC01 fixes what the details show. |
| **TP12** — a non-member creates | It is the permission rule specific to tasks for changes. A failure would let anyone write into any project. | US05 AC06 and DoD03 give "Project not found." and say no task is created. SEC02 restricts changes to members. |
| **TP13** — a non-member reads | It is the same rule for reading. A failure would leak another project's tasks. | US06 AC03, US20 AC02 and DoD03 give "Project not found." and say no data is shown. |
| **TP16** — a visitor who isn't logged in | It is the other kind of user without permission: without it, every other permission check could be bypassed by not logging in. | DoD02 and SEC01 refuse access with "Please log in to continue.". |

## 5. Corrected assumptions

The AI's first draft was reviewed by the user before this file was written.

| # | AI's original suggestion | User's correction |
|---|---|---|
| C1 | The essential "user without permission" should be only the non-member (TP12, TP13). | Both kinds: the non-member and the visitor who isn't logged in. TP16 was added to the selected scenarios. |
| C2 | A request with several invalid fields would be left out of the matrix, because the specification doesn't say which message is shown. | Test it. TP05 checks what the specification does say (the task is refused and nothing is created) and doesn't assert which message is shown (G1). |

**Confirmed without change:**
- TP10 as the essential "create and read" scenario;
- TP06 as the essential invalid-title scenario;
- the reading of DoD05 that also covers a task identifier from another project (TP19);
- the test levels;
- the scope (section 1).

## 6. Specification gaps

| # | Gap | Decision |
|---|---|---|
| G1 | When several fields are invalid at once, the specification doesn't say which message is shown, or whether all of them are. | TP05 only asserts what is specified: one of the fields' messages is shown and no task is created. The order stays undefined until the specification fixes it. |
| G2 | DoD05 speaks of "the project has no task with the given identifier", and doesn't mention a task that exists in another project. | The user confirmed this reading: such a task doesn't exist in this project, so "Task not found." applies (TP19). |
| G3 | SEC03 asks for "the same response" for a project that can't be accessed and one that doesn't exist, without saying what "the same" covers. | TP15 compares the whole answer the user receives, not only the message. |
| G4 | The rule for trimming spaces (D01, D02) has no acceptance criterion of its own in US05. | TP04 derives its trimming cases directly from D01 and D02. |
