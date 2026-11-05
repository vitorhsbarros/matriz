# Specification review (Lab 01, exercise 6)

- **Date:** 2026-11-04
- **Reviewer:** Claude, acting as critical reviewer. The prompt is in [docs/prompts/specification-review.md](../prompts/specification-review.md).
- **Decisions:** Vitor Barros (human in the loop).
- **Status:** closed. Every finding has a decision, and the approved corrections are applied.

## Scope

The Specification Pack: [README.md](../../README.md), [AGENTS.md](../../AGENTS.md), [requirements.md](../requirements.md), [user-stories.md](../user-stories.md) and [constraints.md](../constraints.md).

The only statement from the client is: *"We need an application where our teams can organise projects, manage tasks and follow the progress of the work."* Everything else was proposed with AI assistance and accepted by the project team. This review checks which of it is confirmed, which is assumed, and what is missing.

## Categories and severity

**Categories** (from the lab sheet):
1. Ambiguous or contradictory requirements
2. Unjustified features
3. Assumptions presented as facts
4. Acceptance criteria that aren't verifiable
5. Security or privacy risks
6. Inconsistencies between documents

**Severity:**
- **High:** would lead to a wrong implementation.
- **Medium:** an omission, a weak justification or an unconfirmed decision.
- **Low:** wording.

## Findings

| ID | Category | Severity | Location | Description | Proposed correction | Decision and reason |
|---|---|---|---|---|---|---|
| SR01 | 3 | Medium | `requirements.md` §1 "Problem"; `README.md` intro and "Problem" | The problem says Matriz is for **small teams** that **don't need** sprints, reports or configuration. The client never said this: they described "a company" and "our teams". It is a product assumption presented as a fact. | State the client's need, then mark the rest as assumption ASM12. [Text](#sr01) | **Accepted.** Applied. Also replaced "small teams" with "work teams" in the README and AGENTS.md introductions. |
| SR02 | 3 | Medium | `constraints.md` §5 (C01–C04) | The scope limitations are presented as fixed constraints, but they were decided by the project team to keep the MVP simple. The client hasn't confirmed them. | Say where they come from. [Text](#sr02) | **Rejected:** leave as it is. |
| SR03 | 3 | Medium | `user-stories.md` §3 (D01–D12) | The 12 decisions were proposed with AI assistance to remove ambiguities and accepted without being reviewed one by one. Some change visible behaviour: teams visible to everyone (D05), unique team names (D06), any member can change any task (D08), the team rule checked only when assigning (D09) and due dates (D12). | Record their origin and list the ones to confirm with the client. [Text](#sr03) | **Rejected for now:** "keep it as it is for now". The decisions are in `user-stories.md` §3. D05, D06, D08, D09 and D12 can be confirmed with the client later. |
| SR04 | 1 | Medium | `requirements.md` FR14; `constraints.md` ASM05 | FR14 doesn't say who can be assigned. The rule that a team's members must all belong to the project lives only in **assumption** ASM05, yet US13 AC04 enforces it as confirmed behaviour. | Move the rule into FR14, and leave ASM05 with only the assumption. [Text](#sr04) | **Accepted:** "better to specify who can be assigned". Applied. |
| SR05 | 1 | Low | `requirements.md` NFR06 | "Displayed in under 2 seconds" doesn't say where the time starts and ends, or on which environment. | Define what is measured. [Text](#sr05) | **Accepted.** Applied. |
| SR06 | 2 | Medium | `requirements.md` FR08, FR09; `user-stories.md` US11–US13 | Teams account for 3 of the 13 stories and several rules (D05, D06, D09). Their only justification is "part of the original idea of the application". The client mentioned "our teams", but not assigning tasks to a team. | Give a justification tied to the client's statement and mark it as an assumption (ASM13). Alternatively, move teams to Should. [Text](#sr06) | **Accepted.** Applied. |
| SR07 | 2 | Low | `requirements.md` FR10, FR11 | The due date is stored, edited and validated (D12), but no feature uses it: nothing is ordered, highlighted or filtered by it. Its justification is missing. | Add a justification. Alternatively, remove the due date from the MVP. [Text](#sr07) | **Adjusted:** keep the due date. "It may get future features, and for now it serves as a working reference for the teams in the MVP." Justification added with these words. |
| SR08 | 4 | Medium | `user-stories.md` US01 AC10 | The criterion only checks that the stored data doesn't contain the text "secure123". A reversible encoding (e.g. Base64) would pass, while still letting anyone recover the password (NFR04). | Add two checks that a reversible encoding fails. [Text](#sr08) | **Accepted**, and strengthened: "we should use a more robust method". AC10 applied; PD06 now requires one-way, salted and slow hashing, never reversible encryption. |
| SR09 | 5 | Medium | `requirements.md` §5; `constraints.md` §3 | Nothing limits repeated log-in attempts, so passwords can be guessed by trying many times (brute force). | Record it as a pending decision (PD10). Alternatively, add an NFR. [Text](#sr09) | **Accepted.** Applied (PD10). |
| SR10 | 5 | Medium | `constraints.md` ASM01, PRI04; `user-stories.md` D05 | Sign-up is open to anyone (ASM01), and every user sees all teams and their members' names (D05). Anyone who creates an account can therefore see the names of everyone in a team. | Record the risk, and add a pending decision on who can see teams (PD11). [Text](#sr10) | **Accepted.** Applied (PRI04 risk, PD11). |
| SR11 | 5 | Low | `user-stories.md` US09 AC04, US12 AC03; `constraints.md` SEC04 | "No user found with this email." reveals whether an email has an account. Log-in hides exactly this (SEC04). | Record it as an accepted risk, since only logged-in users can add members. [Text](#sr11) | **Rejected, with an alternative:** "we can't say whether a user with that email exists". The message became the generic "This user could not be added." (SEC09). |
| SR12 | 6 | Low | `AGENTS.md` §5 | It forbids editing `docs/reviews/`, but recording decisions in an open review is part of the review process. | Allow it for open reviews. [Text](#sr12) | **Accepted** after the consequences were explained. Applied. |
| SR13 | 6 | Low | `user-stories.md` DoD06; `constraints.md` C07 | C07 says user stories are technology-independent, but DoD06 refers to ADR-0002, a technical decision. | Remove the reference to ADR-0002 from DoD06. [Text](#sr13) | **Accepted.** Applied. |
| SR14 | 6 | Low | `user-stories.md` US06 title and backlog row | The title "List a project's tasks" no longer matches the board described in FR15 and US06. This is the same item as review-01 S02. | Rename it to "View a project's task board". [Text](#sr14) | **Accepted.** Applied. Also resolves review-01 S02. |

## Proposed corrections

### SR01

**`requirements.md` §1, "Problem".** Replace the paragraph with:

> **Problem.** The client needs "an application where our teams can organise projects, manage tasks and follow the progress of the work". The project assumes these are small teams that don't need the sprints, reports and configuration of tools like Jira (constraints.md, ASM12).

**`constraints.md` §2, "About the product".** Add the row:

> | ASM12 | **Target users:** the client's teams are small and don't need sprints, reports or configurable workflows. | If wrong, the scope limitations C01–C04 must be revisited. |

**`README.md`, "Problem".** Replace the first sentence with:

> The client needs an application where its teams can organise projects, manage tasks and follow the progress of the work. The project assumes these are small teams for whom tools like Jira bring sprints, reports, workflows and configuration they don't need (ASM12).

### SR02

**`constraints.md` §5.** Replace the first paragraph with:

> The application is deliberately simple: a stripped-down alternative to Jira. These features are excluded from every version, not just postponed. The exclusions were decided by the project team, based on assumption ASM12. The client hasn't confirmed them.

### SR03

**`user-stories.md` §3.** Replace the first paragraph with:

> The requirements left some points open. These decisions apply to every US. They were proposed with AI assistance and accepted by the project team. D05, D06, D08, D09 and D12 change behaviour that users see, so they should be confirmed with the client.

### SR04

**`requirements.md` FR14.** Replace the requirement text with:

> The system must let users assign a task to a member of the project, or to a team whose members all belong to the project at that moment, and undo that assignment.

**`constraints.md` ASM05.** Replace the assumption text with:

> **Teams independent of projects:** a team exists outside projects and can work on several. Later changes to a team don't undo its existing assignments (D09).

### SR05

**`requirements.md` NFR06.** Replace the requirement text with:

> When a member opens a project with up to 500 tasks, the complete board is shown within 2 seconds, measured on the demonstration machine with one active user.

### SR06

**`requirements.md` FR08 and FR09, justification column.** Replace with:

> The client's users work in teams ("our teams"). Assigning a task to a team covers work shared by a group, without choosing one person (ASM13).

**`constraints.md` §2, "About the product".** Add the row:

> | ASM13 | **Team assignment:** the client's teams share tasks, so a task can be assigned to a whole team. | If wrong, FR08, FR09 and US11–US13 can move out of the MVP. |

### SR07

**`requirements.md` FR10, justification column.** Replace with:

> It is the core feature of the application. The optional due date lets members see deadlines on the board.

### SR08

**`user-stories.md` US01 AC10.** Replace the scenario with:

```gherkin
Scenario: AC10 — Password not stored in readable text
  Given the visitor signed up with the password "secure123"
  When the stored data for that account is inspected
  Then the text "secure123" does not appear in any of the stored data
  And decoding the stored value as Base64 or hexadecimal does not produce "secure123"
  And a second account created with the password "secure123" stores a different value
```

### SR09

**`constraints.md` §4.** Add the row:

> | PD10 | **Protection against repeated log-in attempts** (e.g. a temporary block after several failures for the same email). Not in the MVP requirements. | review SR09 |

### SR10

**`constraints.md` PRI04.** Append to the rule:

> Risk: with open sign-up (ASM01), anyone who creates an account can see these names.

**`constraints.md` §4.** Add the row:

> | PD11 | **Who can see teams:** every user (D05), or only users who share a project with the team's members. | review SR10 |

### SR11

**`constraints.md` §3, "Requirements".** Add the row:

> | SEC09 | Adding a member by email reveals whether that email has an account (US09 AC04, US12 AC03). Accepted, because only logged-in users can add members. | review SR11 |

### SR12

**`AGENTS.md` §5.** Replace the third bullet with:

> - Don't edit the historical records unless asked: `docs/requirements-v1.md`, `docs/prompts/` and closed reviews in `docs/reviews/`. Recording the user's decisions in an open review is part of that review.

### SR13

**`user-stories.md` DoD06.** Replace the explanation sentence with:

> Applies to every action that changes data while logged in. It protects the user against actions sent from other websites on their behalf.

### SR14

**`user-stories.md`.**
- Rename the heading "US06 — List a project's tasks" to "US06 — View a project's task board".
- Rename the backlog row title in the same way, and update its link to `#us06--view-a-projects-task-board`.

## Decision log

Decisions taken by Vitor Barros on 2026-11-04. Quotes are the user's own reasons.

| ID | Decision | Changes applied |
|---|---|---|
| SR01 | Accepted | `requirements.md` §1 Problem and goal 4. `constraints.md` ASM12. `README.md` intro and Problem. `AGENTS.md` intro ("work teams"). |
| SR02 | Rejected ("leave as it is") | None. |
| SR03 | Rejected for now ("keep it as it is for now") | None. |
| SR04 | Accepted ("better to specify who can be assigned") | `requirements.md` FR14. `constraints.md` ASM05. |
| SR05 | Accepted | `requirements.md` NFR06. |
| SR06 | Accepted | `requirements.md` FR08 and FR09 justifications. `constraints.md` ASM13. |
| SR07 | Adjusted: keep the due date ("it may get future features, and for now it is a working reference for the teams") | `requirements.md` FR10 justification. |
| SR08 | Accepted and strengthened ("a more robust method") | `user-stories.md` US01 AC10. `constraints.md` PD06: a one-way, salted, slow hash, never reversible encryption. |
| SR09 | Accepted | `constraints.md` PD10. |
| SR10 | Accepted | `constraints.md` PRI04 and PD11. |
| SR11 | Rejected, replaced ("we can't say whether a user with that email exists") | Message "No user found with this email." replaced with "This user could not be added." in `user-stories.md` US09 AC04 and US12 AC03, `openapi.yaml` and `api-contract.md`. `constraints.md` SEC09. |
| SR12 | Accepted | `AGENTS.md` §5. |
| SR13 | Accepted | `user-stories.md` DoD06. |
| SR14 | Accepted | `user-stories.md` US06 title and backlog row. `constraints.md` PD09 (review-01 S02 resolved). |

### Checks after applying

- **Only decided corrections applied:** SR02 and SR03 were not applied, as decided.
- **`openapi.yaml`:** still a valid OpenAPI 3.1 document (Redocly CLI).
- **No broken links or anchors** in the documentation, including the renamed US06.
- **No remaining occurrence** of "No user found with this email." outside the historical reviews.
