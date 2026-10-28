# Matriz — Requirements (v2)

Matriz is a project and task management application, similar to Jira but much simpler. It lets several people create and manage projects and tasks, update task statuses and assign tasks to people or teams.

This document refines [requirements.md](requirements.md) (v1). The changes are summarised in section 8. The limits the application must respect, including what is out of scope, are in [constraints.md](constraints.md).

## 1. Priorities

Priorities follow the **MoSCoW** method:

| Priority | Meaning | Where it goes |
|---|---|---|
| **Must** | Without it the application doesn't fulfil its purpose. | MVP |
| **Should** | Important, but the application works without it. | First version after the MVP |
| **Could** | Useful, included if there is time. | Future feature |
| **Won't** | Not included at this stage. Recorded so it isn't lost. | Future feature |

**Core features** are the Must and Should items, i.e. everything in sections 3 and 4. **Future features** are the Could and Won't items (section 6).

## 2. Actors

| Actor | Responsibilities |
|---|---|
| **Visitor** | Person who isn't logged in yet. Can sign up and log in. |
| **User** | Logged-in person. Creates projects and teams, and sees the projects they are a member of and the tasks assigned to them. |
| **Project manager** | User who created a project. Manages the project's details and members, and can delete it. |
| **Project member** | User added to a project. Creates, edits, assigns and updates the status of the project's tasks. |
| **Team manager** | User who created a team. Manages the team's name and members. |

The project manager is also a member of that project, and every role except Visitor is a User.

## 3. Functional requirements

The v1 IDs are kept so the two versions can be compared. The new requirements (FR17 and FR18) come from splitting v1 requirements whose parts had different priorities.

### Accounts

| ID | Requirement | Actor(s) | Priority | Justification |
|---|---|---|---|---|
| FR01 | The system must let a visitor create an account with name, email and password. The email must be unique. | Visitor | Must | Without accounts there is no way to identify who works on each project or to assign tasks to anyone. |
| FR02 | The system must let users log in and log out with email and password. | Visitor, User | Must | It is the basis of access control (NFR02, NFR03). |

### Projects

| ID | Requirement | Actor(s) | Priority | Justification |
|---|---|---|---|---|
| FR03 | The system must let users create a project with a name (required) and a description (optional). Whoever creates the project becomes its manager. | User | Must | Tasks always belong to a project, so without projects there are no tasks. |
| FR04 | The system must let users edit a project's name and description. | Project manager | Should | Fixes mistakes and keeps information up to date, but a project works without being edited. |
| FR05 | The system must let users delete a project, which also deletes all its tasks. | Project manager | Should | Avoids piling up abandoned projects, but isn't needed to validate the idea. |
| FR06 | The system must let users add and remove project members, one at a time. | Project manager | Must | Without members, only the manager works on the project, which goes against the idea of several people collaborating. |
| FR07 | The system must list the projects the user is a member of. | User | Must | It is the user's entry point to their tasks. |
| FR18 | The system must let users add all members of a team to a project at once. | Project manager | Should | Saves effort when a whole team joins a project, but the same result can be reached with FR06. |

### Teams

| ID | Requirement | Actor(s) | Priority | Justification |
|---|---|---|---|---|
| FR08 | The system must let users create a team with a name. Whoever creates the team becomes its manager. | User | Must | Assigning tasks to teams is part of the original idea of the application. |
| FR09 | The system must let users add and remove users from a team. | Team manager | Must | A team without members is useless. |

### Tasks

| ID | Requirement | Actor(s) | Priority | Justification |
|---|---|---|---|---|
| FR10 | The system must let users create a task in a project with a title (required), a description (optional) and a due date (optional). The task starts in the "To do" status. | Project member | Must | It is the core feature of the application. |
| FR11 | The system must let users edit a task's title, description and due date. | Project member | Must | Tasks change often. Without editing, the alternative would be deleting and recreating them, losing their status and assignment. |
| FR12 | The system must let users delete a task. | Project member | Should | Removes tasks created by mistake, but in the MVP a task that no longer matters can be marked as done. |
| FR13 | The system must let users change a task's status to "To do", "In progress" or "Done". | Project member | Must | Following task progress is part of the original idea of the application. |
| FR14 | The system must let users assign a task to a project member or to a team, and undo that assignment. | Project member | Must | Knowing who is responsible for each task is part of the original idea of the application. |
| FR15 | The system must list a project's tasks, showing the status and assignee of each one. | Project member | Must | Without this list there is no way to see the project's work. |
| FR16 | The system must list the tasks assigned to the user, including those assigned to teams they belong to. | User | Should | Helps each person know what they have to do without going through every project, but the information is already available in FR15. |
| FR17 | The system must let users filter a project's tasks by status and by assignee. | Project member | Should | Becomes useful when a project has many tasks, but for a small project the plain list (FR15) is enough. |

## 4. Non-functional requirements

NFR01 (interface language) became constraint [C05](constraints.md). Its ID is not reused, so references to the other NFRs stay valid.

| ID | Requirement | Priority | Justification |
|---|---|---|---|
| NFR02 | Only logged-in users can access projects, teams and tasks. | Must | Project data must not be public. |
| NFR03 | Users can only see and change projects and tasks they are members of. Actions reserved for managers (FR04, FR05, FR06, FR09, FR18) are refused to anyone without that role. | Must | Without this rule, any user could change or delete other people's work. |
| NFR04 | Passwords must never be stored or shown in readable text. | Must | Protects users' accounts in case of a data leak. |
| NFR05 | Deleting a project or a task requires explicit confirmation from the user before it is carried out. | Should | Prevents losing data by mistake. Only applies once FR05 and FR12 are implemented. |
| NFR06 | The task list of a project with up to 500 tasks must be displayed in under 2 seconds. | Should | A slow list makes the application tiresome to use, but in the MVP projects will have few tasks. |
| NFR07 | A change made by one user (e.g. a status change) must be visible to the other project members the next time they open the project or the task. | Must | Without this, several members can't collaborate on the same project. |

## 5. MVP scope

The MVP is the smallest version of Matriz that already lets a team organise work in a project. It includes **all Must requirements**:

- **Accounts:** FR01, FR02
- **Projects:** FR03, FR06, FR07
- **Teams:** FR08, FR09
- **Tasks:** FR10, FR11, FR13, FR14, FR15
- **Non-functional requirements:** NFR02, NFR03, NFR04, NFR07

The MVP must also comply with every constraint in [constraints.md](constraints.md).

With the MVP, a user can sign up, create a project, add members to it, create tasks, assign them to people or teams and follow their status.

**After the MVP** (Should): editing and deleting projects (FR04, FR05), adding a whole team to a project (FR18), deleting tasks (FR12), viewing my tasks (FR16), filtering tasks (FR17), confirmation before deleting (NFR05) and response time (NFR06).

## 6. Future features

Features not included in the planned versions, but which may make sense later. They must still comply with the constraints. Features excluded by a constraint (sprints, reports, integrations, configurability) are not listed here.

| ID | Feature | Priority | Justification |
|---|---|---|---|
| F01 | Notify users in the application when a task is assigned to them. | Could | Ensures nobody misses new work, but FR16 already lets users see their assigned tasks. Notifications by email would be an external integration (C03). |
| F02 | Comment on tasks. | Could | Lets people discuss a task within the application, but the task description covers the essentials. |
| F03 | Search tasks by text in the title or description. | Could | Useful in large projects, which is not the expected case at this stage. |
| F04 | Transfer the manager role of a project or team to another user. | Could | Solves the case where the manager leaves, but it is rare in a small project. |
| F05 | View a task's change history. | Won't | Helps understand who changed what, but adds complexity without being needed to manage tasks. |
| F06 | Configure task statuses per project. | Won't | Excluded by C04: three fixed statuses are enough for a simple project. |
| F07 | Create subtasks or dependencies between tasks. | Won't | Excluded by C04: moves away from the goal of a much simplified application. |

## 7. Assumptions

1. **Open sign-up:** anyone can create an account. There is no system administrator role.
2. **One manager per project and per team:** the manager is whoever creates the project or team, and the role can't be transferred (see F04).
3. **Fixed statuses:** a task always has one of three statuses ("To do", "In progress", "Done"), which can't be configured (C04). Any transition between them is allowed.
4. **One assignee per task:** a task is assigned to at most one person *or* one team, never both. It can also be unassigned.
5. **Teams independent of projects:** a team exists outside projects and can work on several. A task can only be assigned to a team whose members all belong to the project.
6. **Removing members:** when someone leaves a project, the tasks assigned to them in that project become unassigned.

## 8. Changes from v1

| Change | Details |
|---|---|
| Language changed | The application moved from European Portuguese to English. Statuses are named "To do", "In progress" and "Done" (FR10, FR13). |
| Constraints extracted | The out-of-scope items and the interface language (formerly NFR01) moved to [constraints.md](constraints.md) (C01–C08). |
| Priorities and justifications | Every requirement now has a MoSCoW priority and a justification. |
| MVP scope | New section 5, with the Must requirements. |
| Future features | New section 6 (F01–F07). The v1 assumptions about notifications, comments and subtasks became future features. |
| FR06 split | Adding a whole team to a project became FR18 (Should). FR06 only covers individual members (Must). |
| FR15 split | Filtering became FR17 (Should). FR15 only covers the list (Must). |
| IDs translated | Requirement IDs follow English naming: RF → FR, RNF → NFR. Numbers are unchanged. |
