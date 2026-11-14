## Role
You are a senior QA and delivery engineer. You check a delivery against the lab's criteria from the evidence in the repository, not from what earlier reports say, and you treat a claim as met only when you can point to the file, test or run that shows it.

## Goal
Make sure Lab 04's deliverables and completion criteria are all met, fix the gaps, and publish the delivery as a tagged version.

Deliverables:
- tests in the repository, with the commands to run them in `README.md`;
- `docs/test-plan.md`: scenarios, expected results and the tests that check each one;
- `docs/quality-security-review.md`: the code and security review's results, evidence and decisions;
- the final branch or commit, and a clear list of the tests not run and the problems still pending.

Completion criteria:
- at least one unit, one integration and one end-to-end test (or a documented impediment);
- the results shown come from real runs and can be reproduced;
- an authorization scenario between two demo accounts was validated;
- the AI's recommendations were reviewed, and the changes are recorded.

## Context
- **Project:** Matriz, a Java 21, Spring Boot 4, PostgreSQL and React application, built and tested with `./mvnw verify`.
- **Lab 04 so far:**
  - a test plan (`docs/test-plan.md`);
  - unit, integration and end-to-end tests;
  - a code and security review, recorded in `docs/reviews/code-security-review.md`, whose findings were fixed;
  - the records in `docs/verification.md`.
- **Lab 03:** delivered as the Git tag `v1.0.0`.
- **Demo accounts:** Ana, Rui and Sara, created by the seed.

## Constraints
- Check each item against the repository itself: files, test methods, build output.
- First report every deliverable and criterion as met, incomplete or missing, with the evidence, and propose the fixes. Change nothing until the developer approves.
- Don't edit historical records (`docs/prompts/`, closed reviews); a file that moves keeps its history (`git mv`).
- Version numbers follow Semantic Versioning. The developer chooses the version.
- Ask before creating the tag and before pushing (AGENTS.md §6).
- Test names and comments don't cite specification IDs.
- Documentation, code and commit messages are in English.

## Task
1. **Check every deliverable and criterion,** and report each one as met, incomplete or missing, with its evidence. Then propose fixes and wait for approval.
2. **After approval, make the fixes:**
   - **The test plan:** add a column to the scenario matrix in `docs/test-plan.md` naming the automated tests behind each scenario, marking the ones only partly covered.
   - **The review:** move it to `docs/quality-security-review.md`, and update its links and every reference to it.
   - **Reproducibility:** if a validated check (such as the access checks between demo accounts) can't be repeated from the repository, add an automated test that repeats it with the real demo accounts and leaves the data as it was.
   - **The delivery:** add a "Delivery" section to the review, with the version and branch, the tests by level, what wasn't run or automated, and the pending problems and accepted risks; point to it from the README.
3. **Recommend a version** under Semantic Versioning, explaining why, and use the one the developer chooses.
4. **Commit** in small, focused commits.
5. **Ask for confirmation,** then create an annotated tag on the final commit and push the branch and the tag.

## Validation
1. Every test cited in the test plan exists (search for each method).
2. Any new test fails when the rule it checks is broken for one run, and passes when it is restored.
3. `./mvnw verify` passes, with unit, integration and end-to-end tests all run.
4. No reference to the review's old path is left, except in historical records.
5. After the push, the tag on the remote points to the intended commit, and the branch matches the remote.
6. Report as in AGENTS.md §9.
