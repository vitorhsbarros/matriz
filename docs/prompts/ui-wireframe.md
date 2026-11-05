## Goal
Act as the UI designer for Matriz. Use an AI tool to produce a low-fidelity wireframe of two connected screens: the home screen after log-in (my projects, US04) and the project screen (the project's task board, US06). The wireframe must show how the user navigates from one screen to the other, so that the navigation and the elements shown can be checked against the MVP features.

## Context
- **Sources:**
  - the MVP user stories in `docs/user-stories/`, mainly US03–US07, US10 and US13;
  - the exact messages in their acceptance criteria;
  - the data returned by the API in `openapi.yaml`.
- **Home screen (my projects):**
  - the list of projects the user is a member of, sorted alphabetically, with the user's role in each one ("Manager" or "Member");
  - a button to create a project.
- **Project screen:** a board based on "pools" of tasks, with one column per status (To do, In progress, Done):
  - each task is a card showing its title, assignee and due date;
  - moving a card from one column to another changes the task's status;
  - actions to create a task and assign a task.
- **Point of view:** show the screens as seen by a project manager.
- **Empty states:** also show the screens with no data: "You are not a member of any project yet." and "No tasks yet."

## Constraints
- **Tool:** the assistant generates the wireframe itself, as an HTML page, and publishes it as a page with a shareable link.
- **Low fidelity:**
  - greyscale only, plain boxes and real text from the user stories;
  - no brand colours, detailed icons or images.
- **Interaction:** clicking a project on the home screen opens that project's screen. There must also be a way back to the home screen.
- **Device:** desktop only.
- **Language:** all text in English (C05).
- **Scope:** no charts, metrics or statistics (C02). Only MVP features.

## Task
1. Read the MVP user stories and identify the elements and actions of each screen.
2. Generate the low-fidelity HTML wireframe of the two screens:
   - with navigation between them;
   - with a "Show empty state" toggle on each screen that switches between the version with data and the empty version.
3. Publish it as a page with a shareable link, and save a copy in `docs/ui/wireframe.html`.
4. Write a supporting document in `docs/ui/README.md` with:
   - the link to the published page;
   - the screens and the navigation between them;
   - the user stories and acceptance criteria each element covers.

## Validation
Before delivering, check that:
- every element on the screens maps to an MVP user story, and nothing outside the MVP appears;
- navigation works both ways, from the home screen to a project and back;
- messages and status names match the user stories exactly;
- the toggle shows both empty states;
- the published page and the copy in `docs/ui/` are identical.
