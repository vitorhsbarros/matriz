## Role

You are a senior technical presenter and designer who builds short, demo-first talks: few words per slide, one idea per slide, and everything readable from the back of the room.

## Goal

A self-contained HTML slide deck, in Portuguese, for Matriz's 5-minute final presentation, built around the live demo:
- **six moments,** fitting the course's structure:
  1. context;
  2. the hand-off to the live demo, with two flows;
  3. technologies and the main decisions;
  4. methodology and AI;
  5. one challenge: AI also makes mistakes, and the cognitive load of decisions for the human in the loop;
  6. closing, with the repository and application URLs;
- **for the presenter:** speaker notes with the time of each moment, shown only on request (key `N`), and a discreet 5-minute timer.

## Context

- **The course's sheet** (the final presentation's) only sets the structure and the rules: 5 minutes, the five moments, and the demo first. It's not a source of facts about Matriz.
- **The sources of truth are the project's documents.** Every fact, number and example on the slides comes from them:
  - `README.md`, `CHANGELOG.md` and `docs/release-notes/`;
  - `docs/requirements.md`, `docs/user-stories.md` and `docs/constraints.md`, including the pending decisions PD01 to PD13;
  - `docs/architecture.md` and the ADRs, plus `docs/delivery-plan.md` and `docs/deployment-options.md`;
  - `docs/test-plan.md`, `docs/verification.md`, `docs/final-validation.md` and `docs/quality-security-review.md`;
  - `AGENTS.md` and `docs/prompts/`, for the methodology;
  - `docs/ui/README.md` and `frontend/src/styles/tokens.css`, for the visual system.
- **The application** is online at https://matriz-2s4n.onrender.com (`v1.2.0`). The repository is https://github.com/vitorhsbarros/matriz.
- **The live demo,** with two flows:
  1. Ana: the dashboard, *Website*, create a task, move it, assign it, and comment;
  2. the access rules: Rui has no *Edit* or *Delete* on the project, and Sara gets "Project not found.".
- **The challenge:**
  - **AI errors:** only the errors **recorded in the documents**. For example, the `5s` time-out that Hikari refused (`verification.md` §20), and the 409 tests that were thought to be missing but already existed (`verification.md` §19, `final-validation.md`);
  - **The cognitive load of the decisions:** shown with numbers counted in the documents, such as how many pending decisions there were (PD01–PD13) and how many changes are marked "with the user's approval" or "approved by the developer".

## Constraints

- **The file:**
  - a single HTML file, `docs/presentation/index.html`;
  - no external dependencies (no CDN, no web fonts, no images from the internet), so it works without internet in the room;
  - the only external links are the application and the repository.
- **The text:**
  - in Portuguese;
  - at most about 20 words on each slide, with the detail in the speaker notes;
  - one idea per slide, readable from the back of the room.
- **The visual style:** Matriz's colours and typography (`frontend/src/styles/tokens.css`, `docs/ui/README.md`), and readable on a projector.
- **The facts:** only what is in the project's documents (see Context). Every number on the slides is counted or copied from a document, and the document is named in the speaker notes.
- **Security:** no password, token or other secret, not even the demo accounts' passwords; the presenter types them during the demo. The demo accounts are fictitious people (`@example.com`).
- **The QR code:** this machine has no QR code generator, and the file can't depend on the internet. So the closing slide shows the two URLs in large type instead of a QR code.
- **Commits:** one commit with the presentation, the prompt and a link in the README, only after the developer's approval. "Lab 05:" prefix.

## Task

1. Read the sources in Context. Take the numbers and examples from them, each with the document it comes from.
2. Write the script: the six moments, their times, and the speaker notes.
3. Build `docs/presentation/index.html`:
   - navigation with the arrow keys and the space bar, and `N` to show or hide the speaker notes;
   - a discreet 5-minute timer, which starts on the first move forward;
   - `F` for full screen.
4. Write `docs/presentation/demo-script.md`, the demo script for printing: the two flows step by step, and what to say at each step. It also lists what to prepare before the session: open the application a few minutes before, so it wakes up; check the three accounts work; and have plan B ready (running locally with Docker Compose).
5. Check the result (see Validation).
6. Add a link to both files in the README, ask for approval, then commit and push.

## Validation

1. **Facts:** every number and example on the slides is in the document the speaker notes cite.
2. **Offline:** the file makes no request to another site. A search for `http` finds only the two linked URLs.
3. **Navigation:** the arrow keys, the space bar, `N` and `F` work, and the timer starts, checked by opening the file in a headless browser (Playwright's Chromium, already installed).
4. **Readability:** at most about 20 words per slide, counted automatically, and a minimum font size readable on a projector.
5. **Security:** no password or secret: Semgrep `p/secrets` on the new files.
6. **Working tree:** only the two new files, the README and the prompt changed.

If a check fails, fix it and try again, at most twice. Then stop and ask.
