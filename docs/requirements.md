# Matriz — Requirements

This document holds the confirmed requirements of Matriz. Assumptions, constraints, security and privacy rules, and pending decisions are kept apart, in [constraints.md](constraints.md). The MVP user stories, with their acceptance criteria, are in [user-stories.md](user-stories.md).

## 1. Vision and goals

**Vision.** Matriz is a project and task management application, similar to Jira but much simpler. It lets several people create and manage projects and tasks, update task statuses and priorities, assign tasks to people and teams, and discuss them in comments.

**Problem.** The client needs "an application where our teams can organise projects, manage tasks and follow the progress of the work". The project assumes these are small teams that don't need the sprints, reports and configuration of tools like Jira (constraints.md, ASM12).

**Goals:**
1. Let a team organise its work in projects, each with its own tasks and members.
2. Make clear who is responsible for each task: one or more people or teams.
3. Show at a glance where every task stands: To do, In progress or Done.
4. Stay simple enough to use without training, by leaving out what small teams don't need (constraints C01–C04, assumption ASM12).

## 2. User types

| User type | Responsibilities |
|---|---|
| **Visitor** | Person who isn't logged in yet. Can log in with an existing account, such as a demo account (FR21). Signing up is an optional extension (FR01). |
| **User** | Logged-in person. Creates projects and teams, and sees the projects they are a member of on the home dashboard. |
| **Project manager** | User who created a project. Manages the project's details and members, and can delete it. |
| **Project member** | User added to a project. Creates, edits, assigns, comments on and updates the status of the project's tasks. Edits and deletes their own comments. |
| **Team manager** | User who created a team. Manages the team's name and members. |

The project manager is also a member of that project, and every type except Visitor is a User.

## 3. Priorities

Priorities follow the **MoSCoW** method:

| Priority | Meaning | Where it goes |
|---|---|---|
| **Must** | Without it the application doesn't fulfil its purpose. | MVP |
| **Should** | Important, but the application works without it. | First version after the MVP |
| **Could** | Useful, included if there is time. | Future feature |
| **Won't** | Not included at this stage. Recorded so it isn't lost. | Future feature |

## 4. Functional requirements

The IDs are kept from the first version ([requirements-v1.md](requirements-v1.md)) so the versions can be compared. FR17 and FR18 come from splitting requirements whose parts had different priorities.

### Accounts

| ID | Requirement | User type | Priority | Justification |
|---|---|---|---|---|
| FR01 | The system must let a visitor create an account with name, email and password. The email must be unique. | Visitor | Could (optional extension) | Lets new people join without help. Not needed in the MVP, where the demo accounts (FR21) give access. |
| FR02 | The system must let users log in and log out with email and password, and show who is logged in. | Visitor, User | Must | It is the basis of access control (NFR02, NFR03). |
| FR21 | The system must provide demo accounts, created by a seed, so people can log in without signing up. | Visitor | Must | Simplifies access for the demonstration, now that public registration is an optional extension. |

### Projects

| ID | Requirement | User type | Priority | Justification |
|---|---|---|---|---|
| FR03 | The system must let users create a project with a name (required) and a description (optional). Whoever creates the project becomes its manager. | User | Must | Tasks always belong to a project, so without projects there are no tasks. |
| FR04 | The system must let users edit a project's name and description. | Project manager | Must | Part of managing a project's whole life cycle (create, view, edit, delete). Fixes mistakes and keeps the project's information up to date.
| FR05 | The system must let users delete a project, which also deletes all its tasks. | Project manager | Must | Part of managing a project's whole life cycle. Removes finished or abandoned projects, so the project list stays useful.
| FR06 | The system must let users add and remove project members, one at a time. | Project manager | Must | Without members, only the manager works on the project, which goes against the idea of several people collaborating. |
| FR07 | The system must show a home dashboard with the projects the user is a member of, the number of tasks in each status for each project and in total, and a way to open each project. | User | Must | It is the user's entry point to their tasks, and shows at a glance where the work stands (goal 3). |
| FR24 | The system must let users search the projects they are a member of by name. | User | Must | Finds a project quickly when a user belongs to many. |
| FR18 | The system must let users add all members of a team to a project at once. | Project manager | Should | Saves effort when a whole team joins a project, but the same result can be reached with FR06. |

### Teams

| ID | Requirement | User type | Priority | Justification |
|---|---|---|---|---|
| FR08 | The system must let users create a team with a name. Whoever creates the team becomes its manager. | User | Must | The client's users work in teams ("our teams"). Assigning a task to a team covers work shared by a group, without choosing one person (ASM13).
| FR09 | The system must let users add and remove users from a team. | Team manager | Must | The client's users work in teams ("our teams"). Assigning a task to a team covers work shared by a group, without choosing one person (ASM13).
| FR19 | The system must let users rename a team. Team names stay unique. | Team manager | Must | Part of managing a team's whole life cycle (create, view, edit, delete). Keeps the team's name correct as it changes. |
| FR20 | The system must let users delete a team. The team is removed from the assignees of its tasks. | Team manager | Must | Part of managing a team's whole life cycle. Removes teams that no longer exist, so they can't be assigned new tasks. |

### Tasks

| ID | Requirement | User type | Priority | Justification |
|---|---|---|---|---|
| FR10 | The system must let users create a task in a project with a title (required), a description (optional), a due date (optional) and a priority ("Low", "Medium" or "High"; "Medium" if not chosen). The task starts in the "To do" status. | Project member | Must | It is the core feature of the application. The optional due date gives teams a working reference for deadlines in the MVP, and future features may build on it. The priority shows which work matters most.
| FR11 | The system must let users edit a task's title, description, due date and priority. | Project member | Must | Tasks change often. Without editing, the alternative would be deleting and recreating them, losing their status and assignment. |
| FR12 | The system must let users delete a task. | Project member | Must | Part of managing a task's whole life cycle (create, view, edit, delete). Removes tasks created by mistake or no longer needed.
| FR13 | The system must let users change a task's status to "To do", "In progress" or "Done". | Project member | Must | Following task progress is part of the original idea of the application. |
| FR14 | The system must let users assign several eligible assignees to a task, and remove each assignment. An assignee is a member of the project, or a team whose members all belong to the project, at the moment of assignment. | Project member | Must | Knowing who is responsible for each task is part of the original idea of the application. Work is often shared by several people. |
| FR15 | The system must show a project's tasks as a board with one column per status, showing the priority and the assignees of each task. | Project member | Must | Without the board there is no way to see the project's work and where each task stands. |
| FR22 | The system must let users view a task's details: title, description, status, priority, due date, assignees and comments. | Project member | Must | The board shows a summary; the details are where the full task and its discussion are read. |
| FR23 | The system must let users add comments to a task and list them, with their author and date, and let the author edit or delete their own comments. | Project member | Must | Lets members discuss a task inside the application. Editing and deleting keep comments correct. |
| FR16 | The system must list the tasks assigned to the user, including those assigned to teams they belong to, only in projects the user is a member of. | User | Should | Helps each person know what they have to do without going through every project, but the information is already available in FR15. |
| FR17 | The system must let users filter a project's tasks by status and by priority. | Project member | Must | Lets members focus on the work that matters now, using the priority (FR10). |
| FR25 | The system must let users filter a project's tasks by assignee. | Project member | Should | Useful in projects with many people, but the board already shows each task's assignees (FR15). |

## 5. Non-functional requirements

NFR01 (interface language) became constraint C05 in [constraints.md](constraints.md). Its ID is not reused, so references to the other NFRs stay valid. The security and privacy requirements are summarised in [constraints.md, section 3](constraints.md#3-security-and-privacy).

| ID | Requirement | Priority | Justification |
|---|---|---|---|
| NFR02 | Only logged-in users can access projects, teams and tasks. | Must | Project data must not be public. |
| NFR03 | Users can only see and change projects and tasks they are members of. Actions reserved for managers (FR04, FR05, FR06, FR09, FR18, FR19, FR20) are refused to anyone without that role. Editing or deleting a comment is reserved for its author (FR23). | Must | Without this rule, any user could change or delete other people's work. |
| NFR04 | Passwords must never be stored or shown in readable text, including those of the demo accounts. | Must | Protects users' accounts in case of a data leak. |
| NFR05 | Deleting a project, a task, a team or a comment requires explicit confirmation from the user before it is carried out. | Must | Deleting can't be undone. Confirmation prevents losing a project, a task, a team or a comment by mistake (FR05, FR12, FR20, FR23).
| NFR06 | When a member opens a project with up to 500 tasks, the complete board is shown within 2 seconds, measured on the demonstration machine with one active user. | Should | A slow board makes the application tiresome to use, but in the MVP projects will have few tasks. |
| NFR07 | A change made by one user (e.g. a status change) must be visible to the other project members the next time they open the project or the task. | Must | Without this, several members can't collaborate on the same project. |

## 6. MVP scope

The MVP is the smallest version of Matriz that already lets a team organise work in a project. It includes **all Must requirements**:

- **Accounts:** FR02, FR21
- **Projects:** FR03, FR04, FR05, FR06, FR07, FR24
- **Teams:** FR08, FR09, FR19, FR20
- **Tasks:** FR10, FR11, FR12, FR13, FR14, FR15, FR17, FR22, FR23
- **Non-functional requirements:** NFR02, NFR03, NFR04, NFR05, NFR07

The MVP must also comply with every constraint in [constraints.md](constraints.md).

With the MVP, a user can log in with a demo account, see their projects and task counts on the home dashboard, search projects by name, create, edit and delete projects, add members to them, create, rename and delete teams, create, view, edit and delete tasks with a status and a priority, assign them to several people and teams, comment on them, and follow and filter them on the project's board. Projects, teams, tasks and comments therefore have the full set of create, read, update and delete operations.

Project members (FR06) aren't named in the new scope, but they stay in the MVP: they define who is authorised to see a project and who is eligible to be assigned its tasks ([scope reconciliation](reviews/scope-reconciliation.md), SC09).

## 7. Out of the initial scope

**After the MVP (Should):**
- adding a whole team to a project (FR18);
- viewing my tasks (FR16);
- filtering tasks by assignee (FR25);
- response time (NFR06).

**Optional extensions (Could).** Can be added to the MVP if there is time:
- public registration (FR01, story US01 in [user-stories.md, section 6](user-stories.md#6-optional-extensions));
- password recovery (F08).

**Future features (Could and Won't).** Features not included in the planned versions, but which may make sense later. They must still comply with the constraints.

| ID | Feature | Priority | Justification |
|---|---|---|---|
| F01 | Notify users in the application when a task is assigned to them. | Could | Ensures nobody misses new work, but FR16 already lets users see their assigned tasks. Notifications by email would be an external integration (C03). |
| F03 | Search tasks by text in the title or description. | Could | Useful in large projects, which is not the expected case at this stage. |
| F04 | Transfer the manager role of a project or team to another user. | Could | Solves the case where the manager leaves, but it is rare in a small project. |
| F05 | View a task's change history. | Won't | Helps understand who changed what, but adds complexity without being needed to manage tasks. |
| F06 | Configure task statuses per project. | Won't | Excluded by C04: three fixed statuses are enough for a simple project. |
| F07 | Create subtasks or dependencies between tasks. | Won't | Excluded by C04: moves away from the goal of a much simplified application. |
| F08 | Recover a forgotten password. | Could (optional extension) | Lets users regain access without help. Recovery by email would be an external integration, which C03 excludes, so the method must be decided, and C03 revisited, before it is built. |

F02 (comment on tasks) became FR23 and moved into the MVP. Its ID is not reused.

**Excluded from every version:** sprints, reports, external integrations and configurable behaviour (constraints C01–C04).

## 8. Version history

The first version of the requirements is kept in [requirements-v1.md](requirements-v1.md).

| Change | Details |
|---|---|
| Language changed | The application moved from European Portuguese to English. Statuses are named "To do", "In progress" and "Done" (FR10, FR13). |
| Constraints extracted | The out-of-scope items and the interface language (formerly NFR01) moved to [constraints.md](constraints.md). |
| Assumptions extracted | The assumptions moved to [constraints.md, section 2](constraints.md#2-assumptions), so this document only holds confirmed requirements. |
| Priorities and justifications | Every requirement has a MoSCoW priority and a justification. |
| MVP scope and future features | Sections 6 and 7. The v1 assumptions about notifications, comments and subtasks became future features. |
| FR06 and FR15 split | Adding a whole team to a project became FR18, and filtering became FR17 (both Should). |
| Board | FR15 and NFR06 describe the task board agreed in the wireframe (review-01, F06). |
| Full CRUD in the MVP | Editing and deleting projects (FR04, FR05), deleting tasks (FR12) and the confirmation before deleting (NFR05) moved from Should to Must. Renaming and deleting teams were added as FR19 and FR20 (Must). Projects, teams and tasks can now be created, read, updated and deleted in the MVP. |
| IDs translated | RF → FR, RNF → NFR. Numbers are unchanged. |
| Scope reconciliation | The MVP was reconciled with a new scope ([scope-reconciliation.md](reviews/scope-reconciliation.md)). Public registration (FR01) became an optional extension, replaced in the MVP by demo accounts (FR21). Added: viewing the logged-in user (FR02), the home dashboard with task counts (FR07), project search (FR24), task priority (FR10, FR11), several assignees per task (FR14), task details (FR22), comments (FR23, formerly F02) and filtering by status and priority (FR17, now Must). Filtering by assignee became FR25 (Should). Password recovery was added as F08 (optional extension). |
