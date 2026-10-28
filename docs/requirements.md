# Matriz — Requirements (v1)

> **Historical version.** This is the first version of the requirements, kept for reference. The current version is [requirements-v2.md](requirements-v2.md), and the constraints are in [constraints.md](constraints.md).

Matriz is a project and task management application, similar to Jira but much simpler. It lets several people create and manage projects and tasks, update task statuses and assign tasks to people or teams.

**Out of scope:** sprints, reports and integrations with external systems.

## 1. Actors

| Actor | Responsibilities |
|---|---|
| **Visitor** | Person who isn't logged in yet. Can sign up and log in. |
| **User** | Logged-in person. Creates projects and teams, and sees the projects they are a member of and the tasks assigned to them. |
| **Project manager** | User who created a project. Manages the project's details and members, and can delete it. |
| **Project member** | User added to a project. Creates, edits, assigns and updates the status of the project's tasks. |
| **Team manager** | User who created a team. Manages the team's name and members. |

The project manager is also a member of that project, and every role except Visitor is a User. So anything a project member can do, the project manager can do too.

## 2. Functional requirements

### Accounts

| ID | Requirement | Actor(s) |
|---|---|---|
| FR01 | The system must let a visitor create an account with name, email and password. The email must be unique. | Visitor |
| FR02 | The system must let users log in and log out with email and password. | Visitor, User |

### Projects

| ID | Requirement | Actor(s) |
|---|---|---|
| FR03 | The system must let users create a project with a name (required) and a description (optional). Whoever creates the project becomes its manager. | User |
| FR04 | The system must let users edit a project's name and description. | Project manager |
| FR05 | The system must let users delete a project, which also deletes all its tasks. | Project manager |
| FR06 | The system must let users add and remove project members, one at a time or by adding all members of a team at once. | Project manager |
| FR07 | The system must list the projects the user is a member of. | User |

### Teams

| ID | Requirement | Actor(s) |
|---|---|---|
| FR08 | The system must let users create a team with a name. Whoever creates the team becomes its manager. | User |
| FR09 | The system must let users add and remove users from a team. | Team manager |

### Tasks

| ID | Requirement | Actor(s) |
|---|---|---|
| FR10 | The system must let users create a task in a project with a title (required), a description (optional) and a due date (optional). The task starts in the "To do" status. | Project member |
| FR11 | The system must let users edit a task's title, description and due date. | Project member |
| FR12 | The system must let users delete a task. | Project member |
| FR13 | The system must let users change a task's status to "To do", "In progress" or "Done". | Project member |
| FR14 | The system must let users assign a task to a project member or to a team, and undo that assignment. | Project member |
| FR15 | The system must list a project's tasks and let users filter them by status and by assignee. | Project member |
| FR16 | The system must list the tasks assigned to the user, including those assigned to teams they belong to. | User |

## 3. Non-functional requirements

| ID | Requirement |
|---|---|
| NFR01 | All of the system's interface and messages must be in European Portuguese. |
| NFR02 | Only logged-in users can access projects, teams and tasks. |
| NFR03 | Users can only see and change projects and tasks they are members of. Actions reserved for managers (FR04, FR05, FR06, FR09) are refused to anyone without that role. |
| NFR04 | Passwords must never be stored or shown in readable text. |
| NFR05 | Deleting a project or a task requires explicit confirmation from the user before it is carried out. |
| NFR06 | The task list of a project with up to 500 tasks must be displayed in under 2 seconds. |
| NFR07 | A change made by one user (e.g. a status change) must be visible to the other project members the next time they open the project or the task. |

## 4. Assumptions

1. **Open sign-up:** anyone can create an account. There is no system administrator role.
2. **One manager per project and per team:** the manager is whoever creates the project or team, and the role can't be transferred.
3. **Fixed statuses:** a task always has one of three statuses ("To do", "In progress", "Done"), which can't be configured. Any transition between them is allowed.
4. **One assignee per task:** a task is assigned to at most one person *or* one team, never both. It can also be unassigned.
5. **Teams independent of projects:** a team exists outside projects and can work on several. A task can only be assigned to a team whose members all belong to the project. To make that possible, the manager adds the team to the project (FR06).
6. **Removing members:** when someone leaves a project, the tasks assigned to them in that project become unassigned.
7. **No task hierarchy:** there are no subtasks, task dependencies, comments or attachments.
8. **No notifications:** users don't receive alerts, for example when a task is assigned to them. They check their tasks through FR16.
