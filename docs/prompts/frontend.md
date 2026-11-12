## Role

You are a senior front-end developer experienced in React with TypeScript and hand-written CSS. You turn a low-fidelity wireframe into a consistent, accessible interface, and you check every screen against the acceptance criteria.

## Goal

Every MVP feature can be used through the browser application, connected to the real API, with an interface that follows the wireframe's structure and navigation and an appealing visual system of its own:
- **Screens (or equivalent components):**
  - **Log-in:** form, validation and error messages.
  - **Home dashboard:** the logged-in user, the projects they can access, and the task counts per status.
  - **Projects:** list and search, create and open, edit and delete.
  - **Project details:** the project's data, its task board with filters, and the creation forms. Also the members, as the manager's screen (US09).
  - **Task details:** its data, editing, status, priority, assignees, comments and deleting.
  - **Teams** (US11–US13, US17, US18): the wireframe has no screen for them, so they reuse its patterns.
- **Visual system:** inspired by the visual language of Apple's 1980s print advertising: typefaces, colour palette and layout. Applied consistently to every screen through shared components and styles, in place of the wireframe's greyscale. It includes an original logo for Matriz in that spirit.
- **Layouts:** adapted to different screen widths, in light and dark mode.

## Context

- **Project:** Matriz, a simplified Jira-like project and task management application. It is a modular monolith in Java 21 and Spring Boot 4 with PostgreSQL, and a React 19 + TypeScript browser application built with Vite and served from the same JAR (`frontend/`). The stack is in `docs/architecture.md`, section 5.
- **What already exists:**
  - **Back end:** complete. All 30 operations of `openapi.yaml` are implemented and tested: sessions, projects, members, teams, tasks, assignees and comments.
  - **Browser application:** increments I1–I3, without styling. It has:
    - `App.tsx`, with routing through the address's `#`;
    - `api.ts`, the API client with the CSRF token kept in memory;
    - `LogInPage`, `Dashboard`, `ProjectPage`, `Board`, `TaskPage`, `ProjectForm`, `TaskForm` and `ConfirmDelete`;
    - tests with the `fakeApi` helper (Vitest and Testing Library).
  - **Not in the interface yet:** members, teams, assignees and comments. The board and the task details still show "Unassigned".
- **Read first:**
  - `AGENTS.md`;
  - `docs/implementation-plan.md` (increments I4–I6, whose back end is done);
  - the 23 MVP stories in `docs/user-stories.md`, with DoD01–DoD07 and D01–D14;
  - `openapi.yaml` and `docs/api-contract.md` (section 4 and assumptions AS01–AS23);
  - `docs/ui/README.md` and `docs/ui/wireframe.html` (also published at https://claude.ai/artifact/SXbV7V1YMMJbqXWjFDjd4x).
- **Visual reference:** https://www.bygonely.com/apple-vintage-ads-1970s-and-1980s/, a collection of Apple's print ads from the 1970s and 1980s.
- **API address:** the browser application calls `/api` on its own site, because the session cookie is `SameSite=Lax` (ADR-0002). The `http://localhost:8080/api` in `openapi.yaml` is only an example, not a running service.

## Constraints

- **Scope:**
  - this is the next increment of the validated plan: the browser application for every MVP story, and the visual system applied to every screen;
  - stay within the stories and the contract: no new features, fields or endpoints (AGENTS.md §3);
  - if a story can't be completed because of a back-end gap, report it instead of changing the back end.
- **Visual reference, not a copy:**
  - take inspiration from the style of the reference ads (typography, colour palette, layout, tone), but copy nothing verbatim: no images, copy text, slogans or layouts lifted from them;
  - don't use Apple's name, logo, the apple shape or its six-colour stripes as they appear in the logo, so the interface doesn't imitate Apple;
  - the Matriz logo is original, made in SVG.
- **Fonts:**
  - use only fonts with an open licence (e.g. SIL Open Font License);
  - serve them with the application. The browser must make no requests to external services such as Google Fonts (PRI02, C03).
  - Adding font files counts as a new asset that needs approval in the plan.
- **Stack:** React + TypeScript with hand-written CSS, reusing the existing components. Don't add any dependency (CSS framework, router, UI library, icon library) without explicit approval (AGENTS.md §6).
- **Consistency:**
  - shared components and styles;
  - forms and messages look and behave the same everywhere;
  - the colours and type sizes come from design tokens (CSS custom properties) defined once;
  - light and dark mode.
- **Responsive:** every screen works from phone width (about 360 px) to desktop, without horizontal page scrolling. Wide content (tables, the board) scrolls inside its own container or stacks.
- **API:**
  - every action uses the real API;
  - the back-end address is configured in one place (default `/api`, same site), with no hard-coded host;
  - the application code has no mock data. Tests may stub the network.
- **Interface states:**
  - every screen handles loading, empty, success and error;
  - when a submission fails, the data the user entered stays in the form.
- **Requests:**
  - a form or button can't send the same request twice while one is in progress;
  - the interface is updated from the response the API confirmed, never by guessing, so no record is duplicated.
- **Synchronisation:** after creating, editing or deleting, every list, filter and count that depends on the change is updated without a page reload. Totals cover the relevant data set, not only what is visible: the dashboard totals ignore the search (D14).
- **Authorization in the interface:**
  - actions are shown only to whom the stories allow (manager-only actions, author-only comment actions);
  - the API's refusal is always shown when it happens anyway.
- **Messages:** use the texts exactly as in the acceptance criteria (DoD04). Keep all interface text in English (DoD01).
- **Accessibility:**
  - labels on every field;
  - keyboard access for every action, including moving tasks without drag and drop;
  - visible focus;
  - text and controls with at least WCAG AA contrast in both modes.
- **Approval:** present a short plan before changing code, and wait for approval (AGENTS.md §4). The plan includes:
  - the visual system;
  - the screens and navigation;
  - the files;
  - the checks.
- **Conventions:** code, comments and documents in English. Two-space indentation in TypeScript and CSS.

## Task

1. **Prepare.** Read the documents listed in Context, and study the reference site. Then present, and wait for approval:
   - **the visual analysis:** the typefaces (serif or sans, weight, condensed or not, how headlines and body copy are set), the colour palette, the layout and the tone you see in the ads;
   - **the proposed visual system:**
     - the open-licence fonts to use instead;
     - the colour tokens for light and dark mode, with their contrast ratios;
     - the type scale;
     - the spacing;
     - the main components;
     - a description or sketch of the Matriz logo;
   - **the screens and navigation**, including where members and teams go;
   - **the files to create or change**, the font files to add, and any gap found in the stories or the contract;
   - **the checks.**
2. **Visual foundation:**
   - the tokens and base styles;
   - the self-hosted fonts;
   - the logo and favicon;
   - the shared components (buttons, fields and forms, panels, tables, pills, the confirmation box, the loading, empty and error states, the page shell with header and breadcrumbs).
3. **API client:** one place for the back-end address, and typed calls for the operations still missing in `api.ts` (members, teams, eligible assignees, assignees, comments).
4. **Screens:**
   - restyle the existing screens;
   - add:
     - the project's members (list for every member; add and remove for the manager);
     - the teams screen (list, create, rename, delete, and add and remove members);
     - on the task details, the assignees (eligible people and teams; add and remove) and the comments (list, add, edit and delete one's own with confirmation, "(edited)");
     - the assignees on the board's cards.
5. **States and synchronisation:** loading, empty, success and error on every screen, kept form data on failure, no double submissions, and every dependent list and count refreshed after a change.
6. **Documentation:**
   - `docs/implementation-plan.md`: the status, results and differences of I4–I6;
   - `docs/ui/README.md`: a short section on the visual system and the screens beyond the wireframe;
   - README.md: what can be done now.
7. **Report** as in AGENTS.md §9:
   - a summary of the diff;
   - the real results of every check;
   - the problems left unresolved.

   Don't commit. Wait for the user to ask for it.

## Validation

1. **Browser application tests** (`npm test`), for every screen:
   - the loading, empty, success and error states;
   - the exact messages;
   - that the entered data is kept when a submission fails;
   - that a second click doesn't send a second request;
   - that lists and counts are updated after creating, editing and deleting;
   - that actions are shown only to the allowed role.
2. **Full build:** `./mvnw verify` passes, including the back-end tests and `ModularityTest`.
3. **End-to-end check in a real browser** against the real API and database (`docker compose up -d --wait`, then `SERVER_PORT=8081` if port 8080 is in use). Go through the main scenario of every MVP story with the demo accounts:
   - Ana: manager of *Website*;
   - Rui: member of *Website*, manager of *App*;
   - Sara: not a member of *Website*.

   Include at least:
   - log-in and log-out;
   - create, search, edit and delete a project;
   - add and remove a member;
   - create a team, add a member and assign it;
   - create, move, filter, edit and delete a task;
   - assign and remove people;
   - add, edit and delete a comment;
   - Sara getting "Project not found.";
   - the dashboard counts following the changes;
   - everything still there after restarting the back end.
4. **Visual check:**
   - screenshots of every screen at desktop and phone width, in light and dark mode, compared with the wireframe's structure;
   - no horizontal page scrolling;
   - no request to any external host (checked in the browser's network log).
5. **Accessibility check:**
   - every action can be done with the keyboard alone;
   - every field has a label;
   - the contrast ratios of the colour tokens meet WCAG AA.
6. **Report** each check and its real result, with the screenshots' location. Say what couldn't be checked.
