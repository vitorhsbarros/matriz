## Goal
Produce a brief comparison of three architecture alternatives for the Matriz MVP: a **monolith**, a **modular monolith** and **microservices**. The comparison must consider the MVP scope, so that I can choose one of them.

## Context
- **Team:** a single developer (me), with experience in Java and Spring Boot.
- **Deployment:** the application runs locally. For the academic assessment, I must demonstrate it working on a server, which can be my own machine.
- **Scale:** few users, around 25.
- **Academic context:** this is an academic project. The course doesn't require any specific architectural style or pattern.
- **Timeline:** no fixed deadline. Assume a short academic timeline of a few weeks.

## Task
This task has two stages, separated by my decision.

**Stage 1: alternatives**
1. Read the requirements, constraints and user stories.
2. Describe the three alternatives (monolith, modular monolith, microservices). Use only these three, not others.
3. For each one, give a short description, a container diagram, and its strengths and weaknesses for the MVP.
4. Compare them in a table using these criteria:
   - development effort for a single developer;
   - ease of running and demonstrating it on a local server;
   - operational complexity;
   - ease of evolving towards the Should requirements.
5. Keep it brief. Deliver only the alternatives and the comparison, in `docs/architecture/03-alternatives.md`, without the drivers and domain model documents. Record any assumption in the alternatives document.
6. Stop and wait for my decision. Don't recommend any alternative.

**Stage 2: after I choose**
7. For the alternative I chose, identify the main components and the responsibilities of each one, in `docs/architecture/04-solution-architecture.md`.
8. Record the decision and its justification in an ADR, in `docs/architecture/adr/ADR-0001-<short-title>.md`.
