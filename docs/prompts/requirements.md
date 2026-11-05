## Goal
Act as the requirements analyst for Matriz, a new project and task management application. Your goal is to elicit, analyse and document the application's requirements.

## Context
I'm building Matriz, a project and task management application, similar to Jira but much simpler.
The idea is that several people can:
- create and manage projects and tasks (each task belongs to a project);
- update the status of tasks;
- assign tasks to people or teams.

## Constraints
- **Scope:** the application is deliberately simple. Sprints, reports and integrations with external systems are out of scope.
- **No technical decisions:** don't propose a technology stack, database, architecture or any other implementation choice. Focus only on the requirements.
- **Language:** write in European Portuguese.
- **Format:** numbered functional and non-functional requirements (FR01, FR02…; NFR01, NFR02…).
- **Size:** keep the number of requirements small, suited to a small project. It must cover the essential features without splitting them too finely.
- **Ambiguities:** if something is ambiguous, assume the simplest reasonable option and list the assumptions in a dedicated section at the end of the document.

## Task
1. Identify the actors or roles that interact with Matriz and briefly describe each one's responsibilities.
2. Elicit the application's functional (FR) and non-functional (NFR) requirements.
3. Save the result in `docs/requirements.md`.

## Validation
Before delivering, check that:
- every feature in the Context (creating and managing projects and tasks, updating status, assigning to people or teams) is covered by at least one FR;
- no requirement includes anything out of scope (sprints, reports, integrations) or any technical decision;
- each requirement is clear, atomic and verifiable (no vague terms like "fast" or "easy" without a metric);
- there are no duplicate or contradictory requirements;
- each FR is linked to at least one actor.
